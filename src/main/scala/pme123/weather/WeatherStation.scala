package pme123.weather

import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.raquo.laminar.tags.HtmlTag
import org.scalajs.dom
import org.scalajs.dom.HTMLDivElement

case class WeatherStation(name: String, latitude: Double, longitude: Double)

case class WeatherStationDiff(station1: WeatherStation, station2: WeatherStation, color: String):
  lazy val id = s"${station1.name}-${station2.name}"

/**
 * Function type for calculating forecasts from weather station data
 * Takes a map of station name -> hourly data and returns forecast data
 */
type ForecastCalculator = Map[String, Seq[HourlyDataSet]] => Seq[UrnerseeForecast]

case class WeatherStationGroupDiff(
                                    id: String,
                                    label: String,
                                    threshold: Int,
                                    stationDiffs: Seq[WeatherStationDiff],
                                    windStations: Seq[WeatherStation],
                                    info: Option[ReactiveHtmlElement[HTMLDivElement]] = None,
                                    forecastCalculator: Option[ForecastCalculator] = None
)

