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
  val weatherDataVar = Var(Seq.empty[WeatherStationResponse])
  val weatherHDataVar = Var(Seq.empty[WeatherStationResponse])

  def apply(): HtmlElement =

    // Fetch weather data on component mount
    allStations
      .foreach:
        case station @ WeatherStation(_, latitude, longitude) =>
          HOpenMeteoClient
            .fetchWeatherData(latitude, longitude)
            .foreach: data =>
              weatherHDataVar.update(_ :+ WeatherStationResponse(station, data))
          OpenMeteoClient
            .fetchWeatherData(latitude, longitude)
            .foreach: data =>
              weatherDataVar.update(_ :+ WeatherStationResponse(station, data))
    Seq(altdorf, lugano, zurich)
      .foreach:
        case station @ WeatherStation(_, latitude, longitude) =>
          HOpenMeteoClient
            .fetchWeatherData(latitude, longitude)
            .foreach: data =>
              weatherHDataVar.update(_ :+ WeatherStationResponse(station, data))

    val responseSignal: Signal[Seq[ReactiveHtmlElement[HTMLDivElement]]] =
      weatherDataVar.signal.map: data =>
        stationDiffs.map: d =>
          div(
            div(
              idAttr := d.id,
              onMountUnmountCallback(
                mount = ctx =>
                  WeatherGraph(d, data),
                unmount = _ => ()
              )
            ),
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
    div(
      div(
        children <-- responseSignal
      ),
      div(
        child <-- responseHSignal
      )
    )
  end apply

  private def diff(data1: Seq[HourlyDataSet], data2: Seq[HourlyDataSet]): Seq[HourlyDataSet] =
    data1.zip(data2).map:
      case (d1, d2) =>
        HourlyDataSet(
          time = d1.time,
          temperature_2m = d1.temperature_2m - d2.temperature_2m,
          surface_pressure = d1.surface_pressure - d2.surface_pressure,
          pressure_msl = d1.pressure_msl - d2.pressure_msl,
          wind_speed_10m = d1.wind_speed_10m - d2.wind_speed_10m,
          wind_direction_10m = d1.wind_direction_10m - d2.wind_direction_10m,
          wind_gusts_10m = d1.wind_gusts_10m - d2.wind_gusts_10m
        )
end WeatherView
