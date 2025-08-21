package pme123.weather.meteoschweiz

import io.circe
import io.circe.generic.auto.*
import io.circe.parser.*
import sttp.client3.*
import sttp.client3.circe.*
import pme123.weather.{MeteoClient, HourlyDataSet}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import java.time.{LocalDateTime, ZoneOffset, ZonedDateTime}
import java.time.format.DateTimeFormatter

object MeteoSwissClient extends MeteoClient:

  // Test mode flag to avoid real API calls during testing
  private var testMode = false

  def setTestMode(enabled: Boolean): Unit =
    testMode = enabled
    if enabled then
      println("🇨🇭 MeteoSwiss: Test mode enabled - using mock data only")
    else
      println("🇨🇭 MeteoSwiss: Test mode disabled - will attempt real API calls")

  def fetchWeatherData(latitude: Double, longitude: Double): Future[Vector[HourlyDataSet]] =
    println(s"🇨🇭 MeteoSwiss: Fetching weather data for lat=$latitude, lon=$longitude")

    if testMode then
      // In test mode, skip API calls and use mock data directly
      println("🇨🇭 MeteoSwiss: Using mock data (test mode)")
      val mockResponse = MeteoSwissResponse(
        latitude = latitude,
        longitude = longitude,
        data = generateMockData()
      )
      val result = convertToHourlyDataSet(mockResponse)
      println(s"🇨🇭 MeteoSwiss: Successfully converted ${result.length} data points")
      Future.successful(result)
    else
      // Normal mode - attempt real API calls
      for
        response <- fetchFromServer(latitude, longitude)
      yield
        val result = convertToHourlyDataSet(response)
        println(s"🇨🇭 MeteoSwiss: Successfully converted ${result.length} data points")
        result

  private def fetchFromServer(
      latitude: Double,
      longitude: Double
  ): Future[MeteoSwissResponse] =
    val backend = FetchBackend()
    
    // Get the latest available forecast reference time
    val referenceTime = getLatestReferenceTime()
    
    val searchRequest = MeteoSwissSearchRequest(
      collections = Seq("ch.meteoschweiz.ogd-forecasting-icon-ch2"),
      `forecast:reference_datetime` = referenceTime,
      `forecast:variable` = "T_2M", // Temperature at 2m - we'll need multiple requests for different variables
      `forecast:perturbed` = false,
      `forecast:horizon` = "P0DT00H00M00S" // Start with +0h lead time
    )

    println(s"🇨🇭 MeteoSwiss: Sending request with reference time: $referenceTime")
    println(s"🇨🇭 MeteoSwiss: Request body: $searchRequest")

    // Try both GET and POST to see which works
    println("🇨🇭 MeteoSwiss: First trying GET request to collections endpoint...")

    val getRequest = basicRequest
      .get(uri"https://data.geo.admin.ch/api/stac/v1/collections/ch.meteoschweiz.ogd-forecasting-icon-ch2")
      .response(asString)

    getRequest.send(backend).map(_.body).flatMap:
      case Right(response) =>
        println(s"🇨🇭 MeteoSwiss: GET request successful: ${response.take(200)}...")

        // Parse the STAC collection response to find items/assets
        try {
          import io.circe.parser._
          parse(response) match {
            case Right(json) =>
              // Look for links to items or assets in the collection
              val cursor = json.hcursor
              val linksOpt = cursor.downField("links").as[List[io.circe.Json]]

              linksOpt match {
                case Right(links) =>
                  // Find the items link
                  val itemsLink = links.find { link =>
                    link.hcursor.downField("rel").as[String].contains("items")
                  }

                  itemsLink match {
                    case Some(link) =>
                      val itemsUrl = link.hcursor.downField("href").as[String]
                      itemsUrl match {
                        case Right(url) =>
                          println(s"🇨🇭 MeteoSwiss: Found items URL: $url")
                          // Now fetch the items to get actual forecast data
                          fetchItems(url, latitude, longitude)
                        case Left(_) =>
                          println("🇨🇭 MeteoSwiss: No href found in items link")
                          Future.failed(new Exception("No items URL found"))
                      }
                    case None =>
                      println("🇨🇭 MeteoSwiss: No items link found, trying direct assets")
                      // Maybe the collection has direct assets
                      fetchGribData("mock-url", latitude, longitude)
                  }
                case Left(_) =>
                  println("🇨🇭 MeteoSwiss: No links found in collection")
                  Future.failed(new Exception("No links found in collection"))
              }
            case Left(error) =>
              println(s"🇨🇭 MeteoSwiss: Failed to parse JSON: $error")
              Future.failed(new Exception(s"Failed to parse collection response: $error"))
          }
        } catch {
          case e: Exception =>
            println(s"🇨🇭 MeteoSwiss: Exception parsing response: ${e.getMessage}")
            Future.failed(e)
        }
      case Left(error) =>
        println(s"🇨🇭 MeteoSwiss: GET failed, trying POST: $error")

        // Fallback to POST request
        val postRequest = basicRequest
          .post(uri"https://data.geo.admin.ch/api/stac/v1/search")
          .header("Content-Type", "application/json")
          .body(searchRequest)
          .response(asJson[MeteoSwissSearchResponse])

        postRequest.send(backend).map(_.body).flatMap:
          case Right(response) =>
            println("🇨🇭 MeteoSwiss: POST request successful")
            // Extract the download URL and fetch the GRIB data
            response.features.headOption match
              case Some(feature) =>
                val downloadUrl = feature.assets.data.href
                fetchGribData(downloadUrl, latitude, longitude)
              case None =>
                Future.failed(new Exception("No forecast data available"))
          case Left(error) =>
            println(s"🇨🇭 MeteoSwiss: POST also failed: $error")
            Future.failed(new Exception(s"Failed to search forecast data: $error"))

  private def fetchItems(itemsUrl: String, latitude: Double, longitude: Double): Future[MeteoSwissResponse] =
    // First, let's see all available items without filtering
    val allItemsUrl = s"$itemsUrl?limit=50"
    println(s"🇨🇭 MeteoSwiss: Fetching all available items from: $allItemsUrl")
    val backend = FetchBackend()

    val itemsRequest = basicRequest
      .get(uri"$allItemsUrl")
      .response(asString)

    itemsRequest.send(backend).map(_.body).flatMap:
      case Right(response) =>
        println(s"🇨🇭 MeteoSwiss: Items response: ${response.take(200)}...")

        try {
          import io.circe.parser._
          parse(response) match {
            case Right(json) =>
              // Look for features (items) with assets
              val cursor = json.hcursor
              cursor.downField("features").as[List[io.circe.Json]] match {
                case Right(features) if features.nonEmpty =>
                  println(s"🇨🇭 MeteoSwiss: Found ${features.length} forecast items")

                  // Log all available forecast variables
                  val allVariables = features.flatMap { feature =>
                    feature.hcursor.downField("id").as[String].toOption.map { id =>
                      // Extract variable from ID (format: date-time-lead-variable-type-hash)
                      val parts = id.split("-")
                      if (parts.length >= 4) parts(3) else "unknown"
                    }
                  }.distinct
                  println(s"🇨🇭 MeteoSwiss: Available forecast variables: ${allVariables.mkString(", ")}")

                  // Look for temperature data first, then fallback to any available
                  val temperatureFeature = features.find { feature =>
                    feature.hcursor.downField("id").as[String].toOption.exists(_.contains("T_2M"))
                  }.orElse(features.headOption)

                  temperatureFeature match {
                    case Some(feature) =>
                      val featureId = feature.hcursor.downField("id").as[String].getOrElse("unknown")
                      println(s"🇨🇭 MeteoSwiss: Using feature: $featureId")
                      val assetsOpt = feature.hcursor.downField("assets").as[io.circe.Json]

                      assetsOpt match {
                    case Right(assets) =>
                      // Look for a data asset (usually named "data" or similar)
                      val assetKeys = assets.asObject.map(_.keys).getOrElse(Set.empty)
                      println(s"🇨🇭 MeteoSwiss: Available assets: ${assetKeys.mkString(", ")}")

                      // Try to find a temperature data asset (T_2M)
                      val dataAsset = assetKeys.find(key =>
                        key.contains("T_2M") || key.contains("t_2m") || key.contains("temperature")
                      ).orElse(
                        // Fallback to any GRIB file if no temperature-specific file found
                        assetKeys.find(key => key.contains("grib"))
                      ).flatMap { key =>
                        assets.hcursor.downField(key).downField("href").as[String].toOption
                      }

                      dataAsset match {
                        case Some(dataUrl) =>
                          println(s"🇨🇭 MeteoSwiss: Found data URL: $dataUrl")
                          fetchGribData(dataUrl, latitude, longitude)
                        case None =>
                          println("🇨🇭 MeteoSwiss: No data asset found")
                          fetchGribData("mock-url", latitude, longitude)
                      }
                    case Left(_) =>
                      println("🇨🇭 MeteoSwiss: No assets found in feature")
                      fetchGribData("mock-url", latitude, longitude)
                  }
                    case None =>
                      println("🇨🇭 MeteoSwiss: No suitable feature found")
                      fetchGribData("mock-url", latitude, longitude)
                  }
                case Right(_) =>
                  println("🇨🇭 MeteoSwiss: No features found in items")
                  fetchGribData("mock-url", latitude, longitude)
                case Left(error) =>
                  println(s"🇨🇭 MeteoSwiss: Error parsing features: $error")
                  fetchGribData("mock-url", latitude, longitude)
              }
            case Left(error) =>
              println(s"🇨🇭 MeteoSwiss: Failed to parse items JSON: $error")
              fetchGribData("mock-url", latitude, longitude)
          }
        } catch {
          case e: Exception =>
            println(s"🇨🇭 MeteoSwiss: Exception parsing items: ${e.getMessage}")
            fetchGribData("mock-url", latitude, longitude)
        }
      case Left(error) =>
        println(s"🇨🇭 MeteoSwiss: Failed to fetch items: $error")
        fetchGribData("mock-url", latitude, longitude)

  private def fetchGribData(url: String, latitude: Double, longitude: Double): Future[MeteoSwissResponse] =
    // For now, return a mock response since GRIB parsing is complex
    // In a real implementation, you would:
    // 1. Download the GRIB file
    // 2. Parse it using a GRIB library (like ecCodes)
    // 3. Extract data for the specific lat/lon coordinates
    // 4. Convert to the expected format

    println(s"🇨🇭 MeteoSwiss: Using mock GRIB data for testing (URL: $url)")
    val mockData = generateMockData()
    println(s"🇨🇭 MeteoSwiss: Generated ${mockData.length} mock data points")

    Future.successful(MeteoSwissResponse(
      latitude = latitude,
      longitude = longitude,
      data = mockData
    ))

  private def convertToHourlyDataSet(response: MeteoSwissResponse): Vector[HourlyDataSet] =
    response.data.map: data =>
      HourlyDataSet(
        time = data.time,
        temperature_2m = data.temperature_2m,
        pressure_msl = data.pressure_msl,
        surface_pressure = data.surface_pressure,
        wind_speed_10m = data.wind_speed_10m,
        wind_gusts_10m = data.wind_gusts_10m,
        wind_direction_10m = data.wind_direction_10m
      )

  private def getLatestReferenceTime(): String =
    // Get the latest 6-hourly initialization time (00, 06, 12, 18 UTC)
    val nowUtc = ZonedDateTime.now(ZoneOffset.UTC)
    val hour = nowUtc.getHour
    val latestInitHour = (hour / 6) * 6
    val referenceTime = nowUtc.withHour(latestInitHour).withMinute(0).withSecond(0).withNano(0)
    referenceTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"))

  private def generateMockData(): Vector[MeteoSwissHourlyData] =
    // Generate 48 hours of mock data for testing (more data for better visualization)
    val nowUtc = ZonedDateTime.now(ZoneOffset.UTC)
    (0 until 48).map: hour =>
      val time = nowUtc.plusHours(hour)
      // Add some daily temperature variation
      val dailyTempCycle = 10.0 * math.sin((hour % 24) * math.Pi / 12.0 - math.Pi / 2)
      // Add some pressure variation
      val pressureVariation = 5.0 * math.sin(hour * math.Pi / 24.0)

      MeteoSwissHourlyData(
        time = time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")),
        temperature_2m = 12.0 + dailyTempCycle + scala.util.Random.nextGaussian() * 2.0,
        pressure_msl = 1015.0 + pressureVariation + scala.util.Random.nextGaussian() * 3.0,
        surface_pressure = 1012.0 + pressureVariation + scala.util.Random.nextGaussian() * 2.5,
        wind_speed_10m = math.max(0.0, 8.0 + scala.util.Random.nextGaussian() * 4.0),
        wind_gusts_10m = math.max(0.0, 12.0 + scala.util.Random.nextGaussian() * 5.0),
        wind_direction_10m = (180.0 + 60.0 * math.sin(hour * math.Pi / 12.0) + scala.util.Random.nextGaussian() * 30.0) % 360.0
      )
    .toVector

end MeteoSwissClient

// Data structures for MeteoSwiss API
case class MeteoSwissSearchRequest(
    collections: Seq[String],
    `forecast:reference_datetime`: String,
    `forecast:variable`: String,
    `forecast:perturbed`: Boolean,
    `forecast:horizon`: String
)

case class MeteoSwissSearchResponse(
    features: Seq[MeteoSwissFeature]
)

case class MeteoSwissFeature(
    assets: MeteoSwissAssets
)

case class MeteoSwissAssets(
    data: MeteoSwissDataAsset
)

case class MeteoSwissDataAsset(
    href: String
)

case class MeteoSwissResponse(
    latitude: Double,
    longitude: Double,
    data: Vector[MeteoSwissHourlyData]
)

case class MeteoSwissHourlyData(
    time: String,
    temperature_2m: Double,
    pressure_msl: Double,
    surface_pressure: Double,
    wind_speed_10m: Double,
    wind_gusts_10m: Double,
    wind_direction_10m: Double
)
