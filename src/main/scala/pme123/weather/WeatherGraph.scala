package pme123.weather

import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.L.*
import plotly.*
import plotly.Plotly.*
import plotly.element.*
import plotly.layout.*

object WeatherGraph:

  def apply(
      stationGroupDiff: WeatherStationGroupDiff,
      resp: Seq[WeatherStationResponse],
      selectedOptions: Set[String]
  ) =
    val data  = resp.head.data
    val times = data.map(_.time)

    def threshold(posNeg: Int) =
      Scatter(times, data.map(_ => posNeg * stationGroupDiff.threshold))
        .withName("Threshold for wind")
        .withLine(Line().withColor(Color.StringColor("blue")).withDash(Dash.Dot))

    def diffScatters =
      stationGroupDiff.stationDiffs
        .filter(d => selectedOptions.contains(d.id))
        .map:
          case WeatherStationDiff(station1, station2, color) =>
            val data1 = resp.filter(_.station == station1).flatMap(_.data)
            val data2 = resp.filter(_.station == station2).flatMap(_.data)
            Scatter(
              data1.map(_.time),
              data1.zip(data2).map:
                case (d1, d2) =>
                  d1.pressure_msl - d2.pressure_msl
            ).withName(s"${station1.name} - ${station2.name}")
              .withLine(Line().withColor(Color.StringColor(color)))

    val plot =
      diffScatters ++
        Seq(
          threshold(1),
          threshold(-1)
        )
    val lay  = Layout().withTitle(s"${stationGroupDiff.label}: Druckdifferenz (hPa)")
    plot.plot(stationGroupDiff.id, lay) // attaches to div element with id 'plot'
  end apply

  def windGraph(stationGroupDiff: WeatherStationGroupDiff, resp: Seq[WeatherStationResponse]) =
    val windStation: WeatherStation = stationGroupDiff.windStation.getOrElse:
      throw new Exception("No wind station defined")

    val data         = resp.filter(_.station == windStation).flatMap(_.data)
    val windScatters =
      val kmhToKn = 0.539957
      Seq(
        Scatter(
          data.map(_.time),
          data.map(_.wind_speed_10m * kmhToKn)
        ).withName("Wind speed (10m)")
          .withLine(Line().withColor(Color.StringColor("green"))),
        Scatter(
          data.map(_.time),
          data.map(_.wind_gusts_10m * kmhToKn)
        ).withName("Wind gust (10m)")
          .withLine(Line().withColor(Color.StringColor("blue"))),
        Scatter(
          data.map(_.time),
          data.map(_.temperature_2m)
        ).withName("Temperature (2m)")
          .withLine(Line().withColor(Color.StringColor("orange")))
      )
    end windScatters

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
      .withTitle(s"${windStation.name}: Wind forecast (knots)")
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
  end windGraph

  def historyGraph(stationGroupDiff: WeatherStationGroupDiff, resp: Seq[WeatherStationResponse]) =
    val windStation: WeatherStation = stationGroupDiff.windStation.getOrElse:
      throw new Exception("No wind station defined")

    val dataStation  = resp.filter(_.station == windStation).flatMap(_.data)
    def diffScatters = stationGroupDiff.stationDiffs.flatMap:
      case WeatherStationDiff(station1, station2, _) =>
        val data1                        = resp.filter(_.station == station1).flatMap(_.data)
        val data2                        = resp.filter(_.station == station2).flatMap(_.data)
        val data: Seq[((Int, Int), Int)] = data1.zip(data2).zip(dataStation)
          .map:
            case d1 -> d2 -> std =>
              ((d1.pressure_msl - d2.pressure_msl).toInt, std)
          .collect:
            case prDiff ->
                std
                if prDiff > 2 =>
              println(s"${std.wind_speed_10m}/ ${std.wind_direction_10m} -> ${
                  std.wind_speed_10m > 10 &&
                    std.wind_direction_10m > 112 &&
                    std.wind_direction_10m < (360 - 112)
                }")

              prDiff -> (
                std.wind_speed_10m > 10 &&
                  std.wind_direction_10m > 112 &&
                  std.wind_direction_10m < (360 - 112)
              )
          .groupBy(_._1)
          .toSeq
          .sortBy(_._1)
          .map:
            case prDiff -> data =>
              val split = data.span(_._2)
              println(s"prDiff: $prDiff -> ${split._1.size} -> ${split._2.size}")
              prDiff -> split._1.size -> split._2.size
        Seq(
          Scatter(
            data.map(_._1._1),
            data.map(_._1._2)
          ).withName(s"Hours with feen")
            .withLine(Line().withColor(Color.StringColor("red"))),
          Scatter(
            data.map(_._1._1),
            data.map(_._2)
          ).withName(s"Hours without feen")
            .withLine(Line().withColor(Color.StringColor("green")))
        )
    val plot         = diffScatters

    val lay = Layout()
      .withTitle(s"${stationGroupDiff.label}: History Data")
    plot.plot("history-" + stationGroupDiff.id, lay) // attaches to div element with id 'plot'
  end historyGraph

end WeatherGraph
