package pme123.weather

import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.raquo.laminar.tags.HtmlTag
import org.scalajs.dom
import org.scalajs.dom.HTMLDivElement

case class WeatherStationDiffData(
    station1: WeatherStationData,
    station2: WeatherStationData,
    color: String
):
  lazy val id = s"${station1.name}-${station2.name}"
end WeatherStationDiffData

case class WeatherStationGroupDiffData(
    id: String,
    label: String,
    threshold: Int,
    stationDiffs: Seq[WeatherStationDiffData],
    windStations: Seq[WeatherStationData],
    info: Option[ReactiveHtmlElement[HTMLDivElement]],
    // Named forecast results, one per registered algorithm, in dropdown display order.
    forecasts: Seq[(String, Seq[UrnerseeForecast])] = Seq.empty
)

case class WeatherStationData(
    station: WeatherStation,
    data: Seq[HourlyDataSet]
):
  lazy val name = station.name
end WeatherStationData

def createWeatherData (data: Seq[WeatherStationData]) =
  WeatherLogger.debug(s"createWeatherData called with ${data.size} stations")
  if data.isEmpty then
    WeatherLogger.debug("Data is empty, returning empty sequence")
    Seq.empty
  else
    WeatherLogger.debug(s"Processing ${data.size} weather stations: ${data.map(_.station.name).mkString(", ")}")
    stationDiffs
      .map: wsGroupDiff =>
        // Calculate a forecast for every registered algorithm
        val stationDataMap = data.map(ws => ws.station.name -> ws.data).toMap
        val forecasts = wsGroupDiff.forecastCalculators.map: (name, calculator) =>
          WeatherLogger.debug(s"Calculating '$name' forecast for ${wsGroupDiff.id}, stations: ${stationDataMap.keys.mkString(", ")}")
          val result = calculator(stationDataMap)
          WeatherLogger.debug(s"Forecast '$name' calculated for ${wsGroupDiff.id}: ${result.size} entries")
          name -> result

        WeatherStationGroupDiffData(
          wsGroupDiff.id,
          wsGroupDiff.label,
          wsGroupDiff.threshold,
          wsGroupDiff.stationDiffs.map:
            case WeatherStationDiff(station1, station2, color) =>
              val data1: Seq[HourlyDataSet] = data.filter(_.station == station1).flatMap(_.data)
              val data2                     = data.filter(_.station == station2).flatMap(_.data)

              WeatherStationDiffData(
                WeatherStationData(station1, data1),
                WeatherStationData(station2, data2),
                color
              ),
          windStations =
            wsGroupDiff.windStations.flatMap: ws =>
              data.filter(d => ws == d.station)
                .map(d => WeatherStationData(d.station, d.data)),
          info = wsGroupDiff.info,
          forecasts = forecasts
        )
