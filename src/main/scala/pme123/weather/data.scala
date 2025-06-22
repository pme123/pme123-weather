package pme123.weather

// urnersee
val lugano     = WeatherStation("Lugano", 46.0101, 8.96)
val zurich     = WeatherStation("Zurich", 47.3667, 8.55)
val andermatt  = WeatherStation("Andermatt", 46.6356, 8.5939)
val altdorf    = WeatherStation("Altdorf", 46.8804, 8.6444)
//val altdorf = WeatherStation("Isleten", 46.9204, 8.5928)
// mittelland
val genf       = WeatherStation("Genf", 46.2376, 6.1092)
val guettingen = WeatherStation("Guettingen", 47.6035, 9.2874)
val bern       = WeatherStation("Bern", 46.9481, 7.4474)
val neuchatel  = WeatherStation("Neuchatel", 46.9918, 6.931)
val mosen      = WeatherStation("Mosen (Sempach)", 47.2449, 8.2263)
// comersee
val lecco      = WeatherStation("Lecco", 45.8559, 9.397)
val colico     = WeatherStation("Colico", 46.132, 9.3771)
val andeer     = WeatherStation("Andeer", 46.6034, 9.4261)
val vaduz      = WeatherStation("Vaduz", 47.1415, 9.5215)
val milan      = WeatherStation("Milan", 45.4643, 9.1895)
// walensee
val muehlehorn = WeatherStation("Mühlehorn", 47.1176, 9.1724)
// gardasee
val malcesine  = WeatherStation("Malcesine", 45.7614, 10.8086)
val torbole    = WeatherStation("Torbole", 45.8695, 10.8756)
val brescia    = WeatherStation("Brescia", 45.4322, 10.2677)
val bolzano    = WeatherStation("Bolzano", 46.4907, 11.3398)
// hyeres
val hyeres     = WeatherStation("Hyeres", 43.1204, 6.1286)
val lyon       = WeatherStation("Lyon", 45.7485, 4.8467)
val monaco     = WeatherStation("Monaco", 43.7333, 7.4167)
// silvaplana
val silvaplana = WeatherStation("Silvaplana", 46.4581, 9.7951)
val maloja     = WeatherStation("Maloja", 46.4039, 9.6949)
val zernez     = WeatherStation("Zernez", 46.6986, 10.0927)
val chiavenna  = WeatherStation("Chiavenna", 46.3206, 9.3982)

// history
val historyDiff: WeatherStationDiff = WeatherStationDiff(
  lugano,
  zurich,
  "red"
)
lazy val altdorfHistory             = WeatherStationGroupDiff(
  "altdorf",
  "Altdorf",
  4,
  Seq(
    historyDiff
  ),
  windStations = Seq(altdorf)
)

lazy val allStations =
  stationDiffs
    .flatMap(std =>
      std.stationDiffs.flatMap(d => Seq(d.station1, d.station2)) ++
        std.windStations
    )
    .distinct

lazy val stationDiffs                      = Seq(
  urnersee,
  mittellandseen,
  WeatherStationGroupDiff(
    "Walensee",
    "Walensee ???",
    4,
    Seq(
      WeatherStationDiff(
        andeer,
        zurich,
        "red"
      ),
      WeatherStationDiff(
        andeer,
        muehlehorn,
        "lightgreen"
      ),
      WeatherStationDiff(
        muehlehorn,
        zurich,
        "blue"
      )
    ),
    windStations = Seq(muehlehorn)
  ),
  WeatherStationGroupDiff(
    "Comersee",
    "Comersee ???",
    4,
    Seq(
      WeatherStationDiff(
        milan,
        vaduz,
        "red"
      ),
      WeatherStationDiff(
        milan,
        andeer,
        "lightgreen"
      ),
      WeatherStationDiff(
        andeer,
        vaduz,
        "blue"
      )
    ),
    windStations = Seq(lecco, colico)
  ),
  WeatherStationGroupDiff(
    "Gardasee",
    "Diff. Brescia - Bolzano > 2hPa Ora | < -2hPa Peler",
    2,
    Seq(
      WeatherStationDiff(
        brescia,
        bolzano,
        "red"
      )
    ),
    windStations = Seq(malcesine, torbole)
  ),
  WeatherStationGroupDiff(
    "Hyeres",
    "Diff Hyeres - Lyon > 5hPa Mistral",
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
    windStations = Seq(hyeres)
  ),
  WeatherStationGroupDiff(
    "Silvaplana",
    s"Silvaplaner See ???",
    2,
    Seq(
      WeatherStationDiff(
        maloja,
        zernez,
        "red"
      )
    ),
    windStations = Seq(silvaplana)
  )
)
lazy val urnersee: WeatherStationGroupDiff = WeatherStationGroupDiff(
  "Urnersee",
  "Diff. Lugano - Zurich > 4hPa South foehn | < -4hPa North foehn",
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
      "blue"
    ),
    WeatherStationDiff(
      altdorf,
      zurich,
      "lila"
    )
  ),
  windStations = Seq(altdorf, lugano, zurich)
)

lazy val mittellandseen: WeatherStationGroupDiff = WeatherStationGroupDiff(
  "Mittellandseen",
  "Diff. Guettingen - Genf > 2hPa Bise | < -2hPa Westwind",
  2,
  Seq(
    WeatherStationDiff(
      guettingen,
      genf,
      "red"
    ),
    WeatherStationDiff(
      guettingen,
      bern,
      "blue"
    ),
    WeatherStationDiff(
      bern,
      genf,
      "lightgreen"
    )
  ),
  windStations = Seq(neuchatel, mosen, guettingen)
)
