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
    info: Option[ReactiveHtmlElement[HTMLDivElement]]
)

case class WeatherStationData(
    station: WeatherStation,
    data: Seq[HourlyDataSet]
):
  lazy val name = station.name
end WeatherStationData

def createWeatherData (data: Seq[WeatherStationData]) =
  if data.isEmpty then
    Seq.empty
  else
    stationDiffs
      .map: wsGroupDiff =>
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
          info = wsGroupDiff.info
        )
