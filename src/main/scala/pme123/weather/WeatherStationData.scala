package pme123.weather

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
    windStation: Option[WeatherStationData]
)

case class WeatherStationData(
    station: WeatherStation,
    data: Seq[HourlyDataSet]
):
  lazy val name = station.name
end WeatherStationData

def createWeatherData (data: Seq[WeatherStationData]) =
  println(s"weatherDataVar changed: ${data.size}")
  if data.isEmpty then
    Seq.empty
  else
    stationDiffs
      .map: wsGroupDiff =>
        println(s"StationDiff: ${wsGroupDiff.id} - ${wsGroupDiff.stationDiffs.size}")
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
          windStation = data.find(d => wsGroupDiff.windStation.contains(d.station)).map(ws => WeatherStationData(ws.station, ws.data))
        )
