package pme123.weather

import be.doeraene.webcomponents.ui5
import be.doeraene.webcomponents.ui5.*
import be.doeraene.webcomponents.ui5.configkeys.*
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.HTMLDivElement

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.scalajs.js.Object.keys
import pme123.weather.openmeteo.OpenMeteoClient
import pme123.weather.meteoschweiz.{MeteoSwissClient, MeteoSwissClientTest}

object WeatherView:
  val weatherDataVar  = Var(Seq.empty[WeatherStationData])
  val weatherHDataVar = Var(Seq.empty[WeatherStationData])
  val isLoadingVar    = Var(true)

  def apply(selectedTabVar: Var[String]): HtmlElement =
    fetchWeatherData()

    val weatherDataSignal: Signal[Seq[WeatherStationGroupDiffData]] =
      weatherDataVar.signal.map(createWeatherData)

    val weatherViewSignal: Signal[ReactiveHtmlElement[HTMLDivElement]] =
      isLoadingVar.signal
        .combineWith(selectedTabVar.signal)
        .combineWith(weatherDataSignal)
        .map: all =>
          val isLoading   = all._1
          val selectedTab = all._2
          val wsDiffs     = all._3
          if isLoading then
            loadingSpinner
          else
            WeatherLogger.debug(s"Selected Tab: $selectedTab")
            if selectedTab == MapView.tabId then
              MapView(selectedTabVar)
            else
              wsDiffs
                .find(d => selectedTab.contains(d.id))
                  .map: wsDiff =>
                    val stationOptions     = Var(wsDiff.stationDiffs.map(_.id))
                    val windStationOptions = Var(WindStationGraph.allNameOptions)
                    div(
                      className := "weather-view",
                      // Forecast panel at the top
                      if wsDiff.forecast.isDefined then
                        div(
                          className := "graph-container",
                          div(
                            className := "card-header",
                            div(className := "card-title", s"Forecast ${wsDiff.id}"),
                            if wsDiff.id == "Urnersee" then
                              WindSpeedExplanationDialog()
                            else
                              emptyNode
                          ),
                          div(
                            idAttr := s"forecast-${wsDiff.id}",
                            onMountUnmountCallback(
                              mount = ctx =>
                                WeatherLogger.debug(s"Mounting forecast for ${wsDiff.id} with ${wsDiff.forecast.get.size} days")
                                ForecastGraph(wsDiff.id, wsDiff.forecast.get),
                              unmount = _ => ()
                            )
                          )
                        )
                      else div(),
                      // Main pressure difference graph
                      div(
                        className := "graph-container",
                        div(className := "card-title", wsDiff.label),
                        child <-- stationOptions.signal.map: opts =>
                          div(
                            idAttr := wsDiff.id,
                            onMountUnmountCallback(
                              mount = ctx =>
                                WeatherGraph(wsDiff, opts),
                              unmount = _ => ()
                            )
                          )
                      ),
                      // Wind stations
                      if wsDiff.windStations.nonEmpty then
                        div(
                          className := "wind-stations",
                          children <-- windStationOptions.signal.map: opts =>
                            wsDiff.windStations.map: st =>
                              WeatherLogger.debug(s"WindStation: ${st.name}")
                              div(
                                className := "graph-container",
                                div(className := "card-title", st.name),
                                div(
                                  idAttr    := s"wind-${st.name}",
                                  onMountUnmountCallback(
                                    mount = ctx =>
                                      WindStationGraph(st, opts),
                                    unmount = _ => ()
                                  )
                                )
                              )
                        )
                      else div(),
                      // Info panel
                      if wsDiff.info.isDefined then
                        div(
                          className := "graph-container",
                          div(className := "card-title", s"Infos ${wsDiff.id}"),
                          wsDiff.info.get
                        )
                      else div()
                    )
                  .getOrElse(div("No Data"))

    div(
      className := "weather-container",
      child <-- weatherViewSignal
    )
  end apply

  private def fetchWeatherData(): Unit =
    // Run MeteoSwiss tests in the console (commented out due to timezone issues)
    // println("🧪 Running MeteoSwiss Client Tests...")
    // MeteoSwissClientTest.runAllTests()

    def fetch(meteoClient: MeteoClient): Future[Seq[WeatherStationData]] =
      Future.sequence(
        allStations
          .map:
            case station @ WeatherStation(_, latitude, longitude) =>
              meteoClient
                .fetchWeatherData(latitude, longitude)
                .map: data =>
                  Some(WeatherStationData(station, data))
                .recover:
                  case ex =>
                    WeatherLogger.warn(s"Failed to fetch ${station.name}: ${ex.getMessage}")
                    None
      ).map(_.flatten)
    // Use OpenMeteo for smooth plots while developing MeteoSwiss
    fetch(OpenMeteoClient).onComplete:
      case Success(data) =>
        WeatherLogger.info(s"Loaded ${data.size} of ${allStations.size} stations")
        weatherDataVar.set(data)
        isLoadingVar.set(false)
      case Failure(ex) =>
        WeatherLogger.error(s"Failed to fetch weather data: ${ex.getMessage}", ex)
        isLoadingVar.set(false)
    // MeteoSwiss testing via button only for now
  //  MeteoSwissClient.setTestMode(false)
  //  fetch(MeteoSwissClient).map:
  //    weatherDataVar.set
  //  fetch(HOpenMeteoClient).map:
  //    weatherHDataVar.set

  end fetchWeatherData

  private def loadingSpinner: ReactiveHtmlElement[HTMLDivElement] =
    div(
      className := "loading-container",
      div(className := "loading-spinner"),
      div(className := "loading-text", "Loading weather data…")
    )

end WeatherView

object WeatherTabs:

  def apply(selectedTabVar: Var[String]) =
    div(
      className := "tabs",
      button(
        className <-- selectedTabVar.signal.map(sel =>
          if sel == MapView.tabId then "tab tab-map active" else "tab tab-map"
        ),
        onClick --> { _ => selectedTabVar.set(MapView.tabId) },
        svg.svg(
          svg.viewBox := "0 0 24 24",
          svg.width   := "14",
          svg.height  := "14",
          svg.fill    := "none",
          svg.stroke  := "currentColor",
          svg.strokeWidth    := "2",
          svg.strokeLineCap  := "round",
          svg.strokeLineJoin := "round",
          svg.polygon(svg.points := "3 6 9 3 15 6 21 3 21 18 15 21 9 18 3 21"),
          svg.line(svg.x1 := "9", svg.y1 := "3", svg.x2 := "9", svg.y2 := "18"),
          svg.line(svg.x1 := "15", svg.y1 := "6", svg.x2 := "15", svg.y2 := "21")
        ),
        span(className := "tab-name", MapView.tabId)
      ),
      stationDiffs.map(stDiff =>
        button(
          className <-- selectedTabVar.signal.map(sel =>
            if sel == stDiff.id then "tab active" else "tab"
          ),
          onClick --> { _ => selectedTabVar.set(stDiff.id) },
          span(className := "tab-dot ok"),
          span(className := "tab-name", stDiff.id)
        )
      )
    )
end WeatherTabs
