package pme123.weather

import com.raquo.laminar.api.L.*
import plotly.*
import plotly.Plotly.*
import plotly.element.*
import plotly.layout.*

object WeatherGraph:

  def apply(
      stationGroupDiff: WeatherStationGroupDiffData,
      selectedOptions: Seq[String]
  ) =
    WeatherLogger.debug(s"WeatherGraph: ${stationGroupDiff.id}")

    val resp  = stationGroupDiff.stationDiffs
    val data  = resp.head.station1.data
    val times = data.map(_.time)

    def threshold(posNeg: Int) =
      Scatter(times, data.map(_ => posNeg * stationGroupDiff.threshold))
        .withName("Threshold for wind")
        .withLine(Line().withColor(Color.StringColor("lightblue")).withDash(Dash.Dot))

    def diffScatters =
      stationGroupDiff.stationDiffs
        .filter(d => selectedOptions.contains(d.id))
        .map:
          case WeatherStationDiffData(station1, station2, color) =>
            Scatter(
              station1.data.map(_.time),
              station1.data.zip(station2.data).map:
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
    val lay  = Layout()
      .withTitle(stationGroupDiff.label)
      .withXaxis(Axis()
        .withTickformat(tickformat))

    plot.plot(stationGroupDiff.id, lay) // attaches to div element with id 'plot'
  end apply

  def historyGraph(stationGroupDiff: WeatherStationGroupDiff, resp: Seq[WeatherStationData]) =
    val windStation: WeatherStation = stationGroupDiff.windStations.headOption.getOrElse:
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
              WeatherLogger.debug(s"${std.wind_speed_10m}/ ${std.wind_direction_10m} -> ${
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
              WeatherLogger.debug(s"prDiff: $prDiff -> ${split._1.size} -> ${split._2.size}")
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
