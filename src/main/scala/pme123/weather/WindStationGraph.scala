package pme123.weather

import plotly.*
import plotly.Plotly.*
import plotly.element.*
import plotly.layout.*

object WindStationGraph:

  def apply(stationGroupDiff: WeatherStationGroupDiffData, selectedOptions: Seq[String]) =
    val windStation = stationGroupDiff.windStation.getOrElse:
      throw new Exception("No wind station defined")

    val data         = windStation.data
    val windScatters =
      allOptions
        .filter((name, _, _) => selectedOptions.contains(name))
        .map: (name, color, toScatter) =>
          Scatter(
            data.map(_.time),
            toScatter(data)
          ).withName(name)
            .withLine(Line().withColor(Color.StringColor(color)))

    val plot = windScatters

    def direction(deg: Double) =
      deg match
      case d if d > (22.5 + 0 * 45) && d <= (22.5 + 1 * 45) => "NE"
      case d if d > (22.5 + 1 * 45) && d <= (22.5 + 2 * 45) => "E"
      case d if d > (22.5 + 2 * 45) && d <= (22.5 + 3 * 45) => "SE"
      case d if d > (22.5 + 3 * 45) && d <= (22.5 + 4 * 45) => "S"
      case d if d > (22.5 + 4 * 45) && d <= (22.5 + 5 * 45) => "SW"
      case d if d > (22.5 + 5 * 45) && d <= (22.5 + 6 * 45) => "W"
      case d if d > (22.5 + 6 * 45) && d <= (22.5 + 7 * 45) => "NW"
      case d if d > (22.5 + 7 * 45) || d <= 22.5            => "N"
      case d                                                =>
        println(s"BAD DATA: $d")
        ""
      end match
    end direction

    val lay = Layout()
      .withTitle(s"${windStation.name}")
      .withAnnotations(
        data.zipWithIndex
          .collect:
            case (d, index) if index % 2 == 0 => d
          .map: d =>
            Annotation()
              .withShowarrow(false)
              .withY(-0.5)
              .withX(d.time)
              .withText(direction(d.wind_direction_10m))
              .withFont(Font().withSize(6).withColor(Color.StringColor("grey")))
      )
    plot.plot("wind-" + stationGroupDiff.id, lay) // attaches to div element with id 'plot'
  end apply

  type ToScatter = Seq[HourlyDataSet] => Seq[Double]
  val kmhToKn                            = 0.539957
  lazy val windSpeedScatter: ToScatter   = _.map(_.wind_speed_10m * kmhToKn)
  lazy val windGustScatter: ToScatter    = _.map(_.wind_gusts_10m * kmhToKn)
  lazy val temperatureScatter: ToScatter = _.map(_.temperature_2m)

  lazy val allOptions = Seq[(String, String, ToScatter)](
    ("Wind speed (10m)", "green", windSpeedScatter),
    ("Wind gust (10m)", "blue", windGustScatter),
    ("Temperature (2m)", "orange", temperatureScatter),
    ("Pressure at Sea Level (hPa - 1000hPa)", "purple", _.map(_.pressure_msl - 1000)),
    ("High Pressure at Sea Level (1020hPa)", "yellow", _.map(_ => 20)),
  )
  lazy val allNameOptions = allOptions.map(_._1)

end WindStationGraph
