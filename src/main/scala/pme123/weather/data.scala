package pme123.weather

// urnersee
val lugano = WeatherStation("Lugano", 46.0101, 8.96)
val zurich = WeatherStation("Zurich", 47.3667, 8.55)
val andermatt = WeatherStation("Andermatt", 46.6356, 8.5939)
val altdorf = WeatherStation("Altdorf", 46.8804, 8.6444)
//val altdorf = WeatherStation("Isleten", 46.9204, 8.5928)
// comersee
val lecco = WeatherStation("Lecco", 45.8559, 9.397)
val andeer = WeatherStation("Andeer", 46.6034, 9.4261)
// gardasee
val brescia = WeatherStation("Brescia", 45.4322, 10.2677)
val bolzano = WeatherStation("Bolzano", 46.4907, 11.3398)
// hyeres
val hyeres = WeatherStation("Hyeres", 43.1204, 6.1286)
val lyon = WeatherStation("Lyon", 45.7485, 4.8467)
val monaco = WeatherStation("Monaco", 43.7333, 7.4167)

// history
val historyDiff: WeatherStationDiff = WeatherStationDiff(
  lugano,
  zurich,
  "red"
)
lazy val altdorfHistory = WeatherStationGroupDiff(
  "altdorf",
  "Altdorf",
  4,
  Seq(
    historyDiff
  ),
  windStation = Some(altdorf)
)

lazy val allStations =
  stationDiffs
    .flatMap(_.stationDiffs)
    .flatMap(d => Seq(d.station1, d.station2))
    .distinct

lazy val stationDiffs = Seq(
  urnersee,
  WeatherStationGroupDiff(
    "Comersee",
    "Comersee",
    4,
    Seq(
      WeatherStationDiff(
        lecco,
        andeer,
        "red"
      )
    ),
    windStation = Some(lecco)
  ),
  WeatherStationGroupDiff(
    "Gardasee",
    "Gardasee",
    2,
    Seq(
      WeatherStationDiff(
        brescia,
        bolzano,
        "red"
      )
    ),
    windStation = None
  ),
  WeatherStationGroupDiff(
    "Hyeres",
    "Mittelmeer - Hyeres",
    5,
    Seq(
      WeatherStationDiff(
        hyeres,
        lyon,
        "red"
      ),
      WeatherStationDiff(
        monaco,
        hyeres,
        "green"
      )
    ),
    windStation = Some(hyeres)
  )
)
lazy val urnersee: WeatherStationGroupDiff = WeatherStationGroupDiff(
  "Urnersee",
  "Urnersee",
  4,
  Seq(
    historyDiff,
    WeatherStationDiff(
      lugano,
      andermatt,
      "orange"
    ),
    WeatherStationDiff(
      lugano,
      altdorf,
      "lightgreen"
    ),
    WeatherStationDiff(
      andermatt,
      zurich,
      "lightblue"
    ),
    WeatherStationDiff(
      altdorf,
      zurich,
      "lila"
    )
  ),
  windStation = Some(altdorf)
)