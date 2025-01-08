package pme123.weather

import be.doeraene.webcomponents.ui5
import be.doeraene.webcomponents.ui5.*
import be.doeraene.webcomponents.ui5.configkeys.*
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.HTMLDivElement

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.scalajs.js.Object.keys

object WeatherView:
  val weatherDataVar  = Var(Seq.empty[WeatherStationData])
  val weatherHDataVar = Var(Seq.empty[WeatherStationData])

  def apply(selectedTabVar: Var[String]): HtmlElement =

    fetchWeatherData()

    val weatherDataSignal: Signal[Seq[WeatherStationGroupDiffData]] =
      weatherDataVar.signal.map(createWeatherData)

    val weatherViewSignal: Signal[ReactiveHtmlElement[HTMLDivElement]] =
      selectedTabVar.signal.combineWith(weatherDataSignal)
        .map: all =>
          val selectedTab = all._1
          val wsDiffs     = all._2
          println(s"Selected Tab: ${all._1}")
          wsDiffs
            .find(d => selectedTab.contains(d.id))
            .map: wsDiff =>
              val stationOptions = Var(wsDiff.stationDiffs.map(_.id))
              val windStationOptions = Var(WindStationGraph.allNameOptions)
              div(
                child <-- stationOptions.signal.map: opts =>
                  div(
                    idAttr := wsDiff.id,
                    onMountUnmountCallback(
                      mount = ctx =>
                        WeatherGraph(wsDiff, opts),
                      unmount = _ => ()
                    )
                  ),
                if wsDiff.stationDiffs.size > 1 then
                  GraphCheckboxGroup(stationOptions)
                else div(),
                hr(),
                div(
                  child <-- windStationOptions.signal.map: opts =>
                    div(
                      idAttr := s"wind-${wsDiff.id}",
                      onMountUnmountCallback(
                        mount = ctx =>
                          wsDiff.windStation.foreach: _ =>
                            WindStationGraph(wsDiff, opts),
                        unmount = _ => ()
                      )
                    )
                ),
                GraphCheckboxGroup(windStationOptions)
              )
            .getOrElse(div("No Data"))

    val responseHSignal: Signal[ReactiveHtmlElement[HTMLDivElement]] =
      weatherHDataVar.signal.map: data =>
        div(
          idAttr := s"history-${altdorfHistory.id}",
          onMountUnmountCallback(
            mount = ctx =>
              altdorfHistory.windStation.foreach: _ =>
                WeatherGraph.historyGraph(altdorfHistory, data),
            unmount = _ => ()
          )
        )

    // val selectedWeatherSignal: Signal[Option[ReactiveHtmlElement[HTMLDivElement]]] =
    //   weatherViewSignal.map(_.headOption)

    div(
      // children <-- selectedWeatherSignal.map(_.toSeq)
      child <-- weatherViewSignal
    )
  end apply

  private def fetchWeatherData(): Unit =
    // Fetch weather data on component mount
    def fetch(meteoClient: MeteoClient) =
      Future.sequence(
        allStations
          .map:
            case station @ WeatherStation(_, latitude, longitude) =>
              meteoClient
                .fetchWeatherData(latitude, longitude)
                .map: data =>
                  WeatherStationData(station, data),
      )
    fetch(OpenMeteoClient).map:
      weatherDataVar.set
  //  fetch(HOpenMeteoClient).map:
  //    weatherHDataVar.set

  end fetchWeatherData
end WeatherView

object WeatherTabs:

  def apply(selectedTabVar: Var[String]) =
    TabContainer(
      _.collapsed := true,
      width := "100%",
      stationDiffs.map(stDiff =>
        TabContainer.tab(
          _.id       := stDiff.id + "-tab",
          _.text     := stDiff.id,
          _.selected := (stDiff.id == "Urnersee")
        )
      ),
      _.events.onTabSelect
        .map(_.detail.tab.id) --> Observer(x =>
        selectedTabVar.set(x.toString.replace("-tab", ""))
      )
    )
end WeatherTabs
