package pme123.weather

case class WeatherStation(name: String, latitude: Double, longitude: Double)

case class WeatherStationDiff(station1: WeatherStation, station2: WeatherStation, color: String):
  lazy val id = s"${station1.name}-${station2.name}"

case class WeatherStationGroupDiff(
                                    id: String,
                                    label: String,
                                    threshold: Int,
                                    stationDiffs: Seq[WeatherStationDiff],
                                    windStations: Seq[WeatherStation]
)

