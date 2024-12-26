package pme123.weather

import scala.concurrent.Future

trait MeteoClient:
  def fetchWeatherData(latitude: Double, longitude: Double): Future[Vector[HourlyDataSet]]
