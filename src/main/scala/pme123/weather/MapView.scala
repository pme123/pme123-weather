package pme123.weather

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom
import org.scalajs.dom.{HTMLDivElement, HTMLInputElement}
import pme123.weather.openmeteo.formatTime

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

// Minimal facade for the Leaflet.js API (loaded via CDN in index.html)
@js.native
@JSGlobal("L")
object Leaflet extends js.Object:
  def map(id: String, options: js.Object): js.Dynamic                  = js.native
  def tileLayer(urlTemplate: String, options: js.Object): js.Dynamic   = js.native
  def marker(latlng: js.Array[Double], options: js.Object): js.Dynamic = js.native
  def divIcon(options: js.Object): js.Dynamic                          = js.native
  def layerGroup(): js.Dynamic                                        = js.native

object MapView:

  val tabId = "Karte"

  // Persist across tab switches - these are user preferences, not scrub position.
  private val unitVar:   Var[String] = Var("kmh")
  private val metricVar: Var[String] = Var("avg")

  private var leafletMap:  js.UndefOr[js.Dynamic] = js.undefined
  private var markerLayer: js.UndefOr[js.Dynamic] = js.undefined

  private val dayFormatter = DateTimeFormatter.ofPattern("EEE d.M")

  // A station can appear in several groups (e.g. as a wind station and in a diff pair) -
  // we just jump to the first group tab that references it.
  private def groupIdForStation(station: WeatherStation): Option[String] =
    stationDiffs
      .find: group =>
        group.windStations.contains(station) ||
          group.stationDiffs.exists(d => d.station1 == station || d.station2 == station)
      .map(_.id)

  // Uses the browser's local clock/parsing directly (avoids java.time zone-database
  // lookups, which aren't available in the Scala.js runtime).
  private def closestIndexToNow(times: Seq[String]): Int =
    if times.isEmpty then 0
    else
      val nowMs = js.Date.now()
      times.zipWithIndex
        .minBy((t, _) => math.abs(new js.Date(t).getTime() - nowMs))
        ._2

  // Same wind speed (km/h) -> color scale as pme123-windalert's MapOverview
  private def windColor(kmh: Double): String =
    if kmh < 12 then "#cccccc"
    else if kmh < 24 then "#44cc00"
    else if kmh < 36 then "#88cc00"
    else if kmh < 48 then "#cccc00"
    else if kmh < 60 then "#cc8800"
    else if kmh < 72 then "#cc3300"
    else if kmh < 84 then "#cc0000"
    else if kmh < 96 then "#c0003c"
    else if kmh < 108 then "#e0006e"
    else "#cc00cc"

  // Beaufort scale, same limits/labels as pme123-windalert's useUnits.ts
  private val bftLimits = Seq(1, 6, 12, 20, 29, 39, 50, 62, 75, 89, 103, 118)

  private def bftFromKmh(kmh: Double): Int =
    bftLimits.indexWhere(kmh < _) match
      case -1 => 12
      case i  => i

  private def kmhToUnit(kmh: Double, unit: String): Double =
    unit match
      case "kn"  => kmh / 1.852
      case "bft" => bftFromKmh(kmh).toDouble
      case _     => kmh

  private def unitLabel(unit: String): String =
    unit match
      case "kn"  => "kn"
      case "bft" => "Bft"
      case _     => "km/h"

  private def fmtWind(kmh: Double, unit: String): String =
    val v = kmhToUnit(kmh, unit)
    if unit == "bft" then math.round(v).toString else f"$v%.1f"

  private def speedFor(h: HourlyDataSet, metric: String): Double =
    if metric == "gust" then h.wind_gusts_10m else h.wind_speed_10m

  // Arrow pointing in wind direction (to direction, i.e. heading+180), colored by speed
  private def arrowIcon(heading: Double, speedKmh: Double): js.Dynamic =
    val color  = windColor(speedKmh)
    val rotate = (heading + 180) % 360
    val html   =
      s"""<div style="width:34px;height:34px;display:flex;align-items:center;justify-content:center;">
         |  <svg width="18" height="18" viewBox="0 0 16 16" style="transform:rotate(${rotate}deg)">
         |    <polygon points="8,1 13,13 8,10 3,13" fill="$color" stroke="#1e293b" stroke-width="1" stroke-linejoin="round"/>
         |  </svg>
         |</div>""".stripMargin
    Leaflet.divIcon(
      js.Dynamic
        .literal(html = html, className = "", iconSize = js.Array(34, 34), iconAnchor = js.Array(17, 17))
        .asInstanceOf[js.Object]
    )

  private def greyDotIcon(): js.Dynamic =
    val html = """<div style="border-radius:50%;width:10px;height:10px;background:#94a3b8;border:1.5px solid #cbd5e1;"></div>"""
    Leaflet.divIcon(
      js.Dynamic
        .literal(html = html, className = "", iconSize = js.Array(10, 10), iconAnchor = js.Array(5, 5))
        .asInstanceOf[js.Object]
    )

  private def tipHtml(name: String, h: HourlyDataSet, unit: String, metric: String): String =
    val u        = unitLabel(unit)
    val avgTxt   = s"&Oslash; ${fmtWind(h.wind_speed_10m, unit)} $u"
    val gustTxt  = s"B&ouml;e ${fmtWind(h.wind_gusts_10m, unit)} $u"
    val avgHtml  = if metric == "avg" then s"<b>$avgTxt</b>" else avgTxt
    val gustHtml = if metric == "gust" then s"<b>$gustTxt</b>" else gustTxt
    s"""<div style="font-size:.82rem;line-height:1.5;min-width:155px">
       |  <b>$name</b><br>
       |  $avgHtml &middot; $gustHtml &middot; ${h.wind_direction_10m.round}&deg;
       |</div>""".stripMargin

  private def toggleGroup(selected: Signal[String], options: Seq[(String, String)], onSelect: String => Unit): HtmlElement =
    div(
      className := "unit-btns",
      options.map: (value, label) =>
        button(
          typ       := "button",
          className <-- selected.map(sel => if sel == value then "unit-btn active" else "unit-btn"),
          onClick --> { _ => onSelect(value) },
          label
        )
    )

  private def dayLegend(times: Seq[String], idxVar: Var[Int], maxIdx: Int): HtmlElement =
    val days = times.zipWithIndex.collect { case (t, i) if i % 24 == 0 => t.take(10) -> i }
    div(
      className := "map-day-legend",
      days.zipWithIndex.map:
        case ((dateStr, dayIdx), i) =>
          val jumpIdx = math.min(dayIdx + 12, maxIdx)
          val label   = LocalDate.parse(dateStr).format(dayFormatter)
          span(
            className := "map-day-item",
            if i > 0 then "| " else "",
            button(
              typ       := "button",
              className := "map-day-btn",
              label,
              onClick --> { _ => idxVar.set(jumpIdx) }
            )
          )
    )

  def apply(selectedTabVar: Var[String]): ReactiveHtmlElement[HTMLDivElement] =
    val stations = WeatherView.weatherDataVar.now()
    val times    = stations.map(_.data.map(_.time)).maxByOption(_.size).getOrElse(Seq.empty)
    val maxIdx   = math.max(0, times.size - 1)
    val idxVar   = Var(closestIndexToNow(times))

    div(
      className := "graph-container map-container",
      div(
        className := "card-header",
        div(className := "card-title", s"Alle Wetterstationen (${allStations.size})"),
        div(
          className := "map-controls",
          toggleGroup(unitVar.signal, Seq("kn" -> "kn", "kmh" -> "km/h", "bft" -> "Bft"), unitVar.set),
          toggleGroup(metricVar.signal, Seq("avg" -> "Mittelwind", "gust" -> "Böen"), metricVar.set)
        )
      ),
      dayLegend(times, idxVar, maxIdx),
      div(
        className := "map-time-control",
        div(
          className := "map-time-label",
          child.text <-- idxVar.signal.map(i => times.lift(i).map(formatTime).getOrElse("—"))
        ),
        input(
          typ       := "range",
          className := "map-time-slider",
          minAttr   := "0",
          maxAttr   := maxIdx.toString,
          value <-- idxVar.signal.map(_.toString),
          onInput --> { ev =>
            val target = ev.target.asInstanceOf[HTMLInputElement]
            idxVar.set(target.value.toInt)
          }
        )
      ),
      div(
        idAttr    := "stations-map",
        className := "stations-map-el",
        onMountUnmountCallback(
          mount = ctx =>
            initMap()
            idxVar.signal
              .combineWith(unitVar.signal)
              .combineWith(metricVar.signal)
              .foreach { case (i, unit, metric) => renderMarkers(stations, i, unit, metric, selectedTabVar) }(using ctx.owner)
          ,
          unmount = _ => destroyMap()
        )
      )
    )
  end apply

  private def initMap(): Unit =
    val lMap = Leaflet.map("stations-map", js.Dynamic.literal(zoomControl = true).asInstanceOf[js.Object])

    val lats = allStations.map(_.latitude)
    val lons = allStations.map(_.longitude)
    lMap.fitBounds(
      js.Array(js.Array(lats.min, lons.min), js.Array(lats.max, lons.max)),
      js.Dynamic.literal(padding = js.Array(20, 20)).asInstanceOf[js.Object]
    )

    Leaflet
      .tileLayer(
        "https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png",
        js.Dynamic
          .literal(attribution = "&copy; OpenStreetMap contributors &copy; CARTO", maxZoom = 19)
          .asInstanceOf[js.Object]
      )
      .addTo(lMap)

    val mLayer = Leaflet.layerGroup()
    mLayer.addTo(lMap)

    leafletMap = lMap
    markerLayer = mLayer

    // Container is hidden (display:none) until this mount callback runs, so Leaflet
    // needs a re-measure once the tab panel is actually visible in the layout.
    dom.window.setTimeout(() => lMap.invalidateSize(), 150)
  end initMap

  private def renderMarkers(
      stations: Seq[WeatherStationData],
      idx: Int,
      unit: String,
      metric: String,
      selectedTabVar: Var[String]
  ): Unit =
    markerLayer.foreach: mLayer =>
      mLayer.clearLayers()

      val dataByStation = stations.map(ws => ws.station -> ws.data).toMap

      allStations.foreach: station =>
        val hourly = dataByStation.get(station).flatMap(_.lift(idx))
        val icon   = hourly match
          case Some(h) => arrowIcon(h.wind_direction_10m, speedFor(h, metric))
          case None    => greyDotIcon()

        val marker = Leaflet.marker(
          js.Array(station.latitude, station.longitude),
          js.Dynamic.literal(icon = icon).asInstanceOf[js.Object]
        )
        hourly.foreach: h =>
          marker.bindTooltip(tipHtml(station.name, h, unit, metric))
        groupIdForStation(station).foreach: groupId =>
          marker.on("click", () => selectedTabVar.set(groupId))

        mLayer.addLayer(marker)
  end renderMarkers

  private def destroyMap(): Unit =
    leafletMap.foreach(_.remove())
    leafletMap = js.undefined
    markerLayer = js.undefined

end MapView
