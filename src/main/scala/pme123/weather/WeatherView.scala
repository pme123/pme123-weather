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
          WeatherLogger.debug(s"Selected Tab: ${all._1}")
          wsDiffs
            .find(d => selectedTab.contains(d.id))
            .map: wsDiff =>
              val stationOptions = Var(wsDiff.stationDiffs.map(_.id))
              val windStationOptions = Var(WindStationGraph.allNameOptions)
              div(
                className := "weather-view",
                div(
                  className := "graph-container",
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
                  else div()
                ),
                if wsDiff.windStations.nonEmpty then
                  div(
                    className := "wind-stations",
                    children <-- windStationOptions.signal.map: opts =>
                      wsDiff.windStations.map: st =>
                        WeatherLogger.debug(s"WindStation: ${st.name}")
                        div(
                          className := "graph-container",
                          idAttr := s"wind-${st.name}",
                          onMountUnmountCallback(
                            mount = ctx =>
                              WindStationGraph(st, opts),
                            unmount = _ => ()
                          )
                        ),
                    GraphCheckboxGroup(windStationOptions)
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
