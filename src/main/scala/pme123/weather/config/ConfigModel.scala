package pme123.weather.config

import pme123.weather.{WeatherStation, WeatherStationDiff, WeatherStationGroupDiff, defaultStationDiffs}

// JSON-serializable projection of a WeatherStation (name/lat/lon only).
case class ConfigStation(name: String, latitude: Double, longitude: Double):
  def toWeatherStation: WeatherStation = WeatherStation(name, latitude, longitude)

object ConfigStation:
  def from(station: WeatherStation): ConfigStation =
    ConfigStation(station.name, station.latitude, station.longitude)

case class ConfigDiff(station1: ConfigStation, station2: ConfigStation, color: String)

// JSON-serializable projection of a "Gebiet" (WeatherStationGroupDiff). `info` and
// `forecastCalculators` are code, not data, so they aren't part of this model - see
// WeatherConfig.fromConfig for how built-in areas get them back.
case class ConfigArea(
    id: String,
    label: String,
    threshold: Int,
    diffs: Seq[ConfigDiff],
    windStations: Seq[ConfigStation],
    hidden: Boolean = false
)

// A named, user-manageable configuration - multiple can exist side by side (see
// ConfigStore), one of them is "active" at any time.
case class WeatherConfig(name: String, areas: Seq[ConfigArea]):
  def visible: WeatherConfig = copy(areas = areas.filterNot(_.hidden))

object WeatherConfig:

  def toConfig(name: String, areas: Seq[WeatherStationGroupDiff]): WeatherConfig =
    WeatherConfig(
      name,
      areas.map: area =>
        ConfigArea(
          id = area.id,
          label = area.label,
          threshold = area.threshold,
          diffs = area.stationDiffs.map: d =>
            ConfigDiff(ConfigStation.from(d.station1), ConfigStation.from(d.station2), d.color)
          ,
          windStations = area.windStations.map(ConfigStation.from)
        )
    )

  // Areas whose id matches a built-in one (e.g. "Urnersee") get their forecast panel/info
  // reattached from the hardcoded defaults, since those aren't representable in JSON.
  def fromConfig(config: WeatherConfig): Seq[WeatherStationGroupDiff] =
    config.areas.map: area =>
      val builtin = defaultStationDiffs.find(_.id == area.id)
      WeatherStationGroupDiff(
        id = area.id,
        label = area.label,
        threshold = area.threshold,
        stationDiffs = area.diffs.map: d =>
          WeatherStationDiff(d.station1.toWeatherStation, d.station2.toWeatherStation, d.color)
        ,
        windStations = area.windStations.map(_.toWeatherStation),
        info = builtin.flatMap(_.info),
        forecastCalculators = builtin.map(_.forecastCalculators).getOrElse(Seq.empty)
      )

end WeatherConfig
