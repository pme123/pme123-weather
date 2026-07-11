package pme123.weather.config

import pme123.weather.{WeatherStation, defaultStationDiffs}
import pme123.weather.meteoschweiz.meteoSwissWeatherStations

// Name/coordinate suggestions for the station fields in ConfigEditorDialog: stations
// already used in the built-in default configuration (covers the non-MeteoSwiss lake
// towns like Lecco, Malcesine, Hyeres, ...) plus every MeteoSwiss automatic station
// (covers most Swiss lakes and larger towns). Picking a known name auto-fills its
// coordinates so they don't have to be looked up and typed by hand.
object KnownStations:

  lazy val all: Seq[WeatherStation] =
    val fromDefaults = defaultStationDiffs.flatMap: area =>
      area.stationDiffs.flatMap(d => Seq(d.station1, d.station2)) ++ area.windStations
    (fromDefaults ++ meteoSwissWeatherStations)
      .groupBy(_.name)
      .map((_, stations) => stations.head)
      .toSeq
      .sortBy(_.name)

  def find(name: String): Option[WeatherStation] =
    val trimmed = name.trim
    if trimmed.isEmpty then None
    else all.find(_.name.equalsIgnoreCase(trimmed))

end KnownStations
