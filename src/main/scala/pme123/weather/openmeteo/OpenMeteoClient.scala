package pme123.weather
package openmeteo

import io.circe
import io.circe.generic.auto.*
import sttp.client3.*
import sttp.client3.circe.*

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


object OpenMeteoClient extends MeteoClient:
  
  def fetchWeatherData(latitude: Double, longitude: Double): Future[Vector[HourlyDataSet]] =
    for
      response <- fetchfromServer(latitude, longitude)
    yield response.hourly.time.zipWithIndex.map:
      case (_, index) =>
        HourlyDataSet(
          time = response.hourly.time(index),
          temperature_2m = response.hourly.temperature_2m(index),
          pressure_msl = response.hourly.pressure_msl(index),
          surface_pressure = response.hourly.surface_pressure(index),
          wind_speed_10m = response.hourly.wind_speed_10m(index),
          wind_gusts_10m = response.hourly.wind_gusts_10m(index),
          wind_direction_10m = response.hourly.wind_direction_10m(index)
        )

  def fetchfromServer(
      latitude: Double,
      longitude: Double,
  ): Future[WeatherResponse] =
    val backend = FetchBackend()
    val url =
      uri"$openMeteoForcastUrl?timezone=Europe/Berlin&latitude=$latitude&longitude=$longitude&hourly=temperature_2m,pressure_msl,surface_pressure,wind_speed_10m,wind_direction_10m,wind_gusts_10m"

    val request = basicRequest.get(url).response(asJson[WeatherResponse])

    request.send(backend).map(_.body).flatMap:
      case Right(response) => Future.successful(response)
      case Left(error) => Future.failed(new Exception(s"Failed to fetch data: $error"))

  end fetchfromServer


    
end OpenMeteoClient
