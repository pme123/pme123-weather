package pme123.weather

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.HTMLDivElement
import plotly.*
import plotly.Plotly.*
import plotly.layout.*

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object WeatherView:
  val weatherDataVar  = Var(Seq.empty[WeatherStationResponse])
  val weatherHDataVar = Var(Seq.empty[WeatherStationResponse])

  def apply(): HtmlElement =

    fetchWeatherData()

    val responseSignal: Signal[Seq[ReactiveHtmlElement[HTMLDivElement]]] =
      weatherDataVar.signal.map: data =>
        println(s"weatherDataVar changed: ${data.size}")
        if data.isEmpty then
          Seq(div("No Weather Data"))
        else
          stationDiffs.map: d =>
            println(s"StationDiff: ${d.id} - ${d.stationDiffs.size}")
            val options = Var(d.stationDiffs.map(_.id).toSet)
            div(
              child <-- options.signal.map: opts =>
                div(
                  idAttr := d.id,
                  onMountUnmountCallback(
                    mount = ctx =>
                      WeatherGraph(d, data, opts),
                    unmount = _ => ()
                  )
                )
            ,
              if d.stationDiffs.size > 1 then
                StationsCheckboxGroup(options)
              else div(),
              div(
                idAttr := s"wind-${d.id}",
                onMountUnmountCallback(
                  mount = ctx =>
                    d.windStation.foreach: _ =>
                      WeatherGraph.windGraph(d, data),
                  unmount = _ => ()
                )
              )
            )
    val responseHSignal: Signal[ReactiveHtmlElement[HTMLDivElement]]     =
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
    div(
      h1("pme123 Weather Experiments"),
      p("Using Data from OpenMeteo (REST APIs)"),
      ul(
        li(div(s"Forcast: $openMeteoForcastUrl")),
        li(s"History: $openMeteoArchiveUrl")
      ),
      div(
        children <-- responseSignal
      ),
      div(
     //   child <-- responseHSignal
      )
    )
  end apply

  private def fetchWeatherData(): Unit =
    // Fetch weather data on component mount
    def fetch(meteoClient: MeteoClient) =
      Future.sequence(
      allStations
        .map:
          case station@WeatherStation(_, latitude, longitude) =>
            meteoClient
              .fetchWeatherData(latitude, longitude)
              .map: data =>
                WeatherStationResponse(station, data),
    )
    fetch(OpenMeteoClient).map:
      weatherDataVar.set
  //  fetch(HOpenMeteoClient).map:
  //    weatherHDataVar.set


  end fetchWeatherData
end WeatherView
