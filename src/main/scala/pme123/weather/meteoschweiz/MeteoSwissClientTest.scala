package pme123.weather.meteoschweiz

import pme123.weather.{WeatherStation, WeatherStationData}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure}

object MeteoSwissClientTest:

  def runAllTests(): Unit =
    println("🧪 Running MeteoSwiss Client Tests...")

    // Enable test mode to avoid real API calls
    MeteoSwissClient.setTestMode(true)

    testClientInterface()
    testParameters()
    testUtils()
    testWeatherDataFetch()
    testConstants()

    // Disable test mode after tests
    MeteoSwissClient.setTestMode(false)

    println("✅ All MeteoSwiss tests completed!")

  def testRealAPI(): Unit =
    println("🌐 Testing MeteoSwiss Real API (this may fail if API is unavailable)...")

    // Disable test mode for real API testing
    MeteoSwissClient.setTestMode(false)

    val zurich = WeatherStation("Zurich", 47.3769, 8.5417)

    MeteoSwissClient.fetchWeatherData(zurich.latitude, zurich.longitude).onComplete:
      case Success(data) =>
        println(s"   ✅ Real API: Fetched ${data.length} data points")
      case Failure(exception) =>
        println(s"   ⚠️  Real API failed (expected): ${exception.getMessage}")

    // Re-enable test mode
    MeteoSwissClient.setTestMode(true)

  def testClientInterface(): Unit =
    println("\n1. Testing MeteoSwiss client interface...")

    // Test that MeteoSwissClient can be used as a MeteoClient
    val client: pme123.weather.MeteoClient = MeteoSwissClient

    if client != null then
      println("   ✅ MeteoSwissClient implements MeteoClient trait")
    else
      println("   ❌ MeteoSwissClient is null")

  def testParameters(): Unit =
    println("\n2. Testing MeteoSwiss parameters...")

    val tests = Seq(
      ("TEMPERATURE_2M", MeteoSwissParameters.TEMPERATURE_2M, "T_2M"),
      ("PRESSURE_MSL", MeteoSwissParameters.PRESSURE_MSL, "PMSL"),
      ("SURFACE_PRESSURE", MeteoSwissParameters.SURFACE_PRESSURE, "PS"),
      ("WIND_SPEED_10M", MeteoSwissParameters.WIND_SPEED_10M, "FF_10M"),
      ("WIND_GUST_10M", MeteoSwissParameters.WIND_GUST_10M, "FX_10M"),
      ("WIND_DIRECTION_10M", MeteoSwissParameters.WIND_DIRECTION_10M, "DD_10M"),
      ("TOTAL_PRECIPITATION", MeteoSwissParameters.TOTAL_PRECIPITATION, "TOT_PREC"),
      ("RELATIVE_HUMIDITY_2M", MeteoSwissParameters.RELATIVE_HUMIDITY_2M, "RELHUM_2M"),
      ("CLOUD_COVER", MeteoSwissParameters.CLOUD_COVER, "CLCT")
    )

    tests.foreach { case (name, actual, expected) =>
      if actual == expected then
        println(s"   ✅ $name = $expected")
      else
        println(s"   ❌ $name: expected $expected, got $actual")
    }

  def testUtils(): Unit =
    println("\n3. Testing MeteoSwiss utilities...")

    // Test initialization time format
    val initTime = getLatestInitializationTime()
    if initTime.endsWith("Z") && initTime.contains("T") then
      println(s"   ✅ Initialization time format: $initTime")
    else
      println(s"   ❌ Invalid initialization time format: $initTime")

    // Test horizon strings
    val horizonTests = Seq(
      (0, "P0DT00H00M00S"),
      (6, "P0DT06H00M00S"),
      (24, "P0DT24H00M00S"),
      (120, "P0DT120H00M00S")
    )

    horizonTests.foreach { case (hours, expected) =>
      val actual = getHorizonString(hours)
      if actual == expected then
        println(s"   ✅ Horizon for ${hours}h: $expected")
      else
        println(s"   ❌ Horizon for ${hours}h: expected $expected, got $actual")
    }

    // Test forecast horizons
    val horizons = getForecastHorizons(5)
    if horizons.length == 5 && horizons.head == "P0DT00H00M00S" then
      println(s"   ✅ Generated ${horizons.length} forecast horizons")
    else
      println(s"   ❌ Forecast horizons generation failed")

  def testWeatherDataFetch(): Unit =
    println("\n4. Testing weather data fetch...")

    // Test with Zurich coordinates
    val zurich = WeatherStation("Zurich", 47.3769, 8.5417)

    MeteoSwissClient.fetchWeatherData(zurich.latitude, zurich.longitude).onComplete:
      case Success(data) =>
        if data.nonEmpty then
          val firstPoint = data.head
          println(s"   ✅ Fetched ${data.length} data points for ${zurich.name}")
          println(s"   📊 Sample data - Time: ${firstPoint.time}")
          println(s"   📊 Sample data - Temp: ${firstPoint.temperature_2m}°C")
          println(s"   📊 Sample data - Pressure: ${firstPoint.pressure_msl} hPa")
          println(s"   📊 Sample data - Wind: ${firstPoint.wind_speed_10m} km/h")

          // Validate data ranges
          val tempValid = firstPoint.temperature_2m > -50 && firstPoint.temperature_2m < 50
          val pressureValid = firstPoint.pressure_msl > 900 && firstPoint.pressure_msl < 1100
          val windValid = firstPoint.wind_speed_10m >= 0

          if tempValid && pressureValid && windValid then
            println("   ✅ Data values are within realistic ranges")
          else
            println("   ⚠️  Some data values are outside realistic ranges")
        else
          println("   ❌ No data points returned")

      case Failure(exception) =>
        println(s"   ❌ Failed to fetch data: ${exception.getMessage}")

  def testConstants(): Unit =
    println("\n5. Testing API constants...")

    if meteoSwissApiUrl == "https://data.geo.admin.ch/api/stac/v1/search" then
      println("   ✅ MeteoSwiss API URL is correct")
    else
      println(s"   ❌ Incorrect API URL: $meteoSwissApiUrl")

    if iconCh2Collection == "ch.meteoschweiz.ogd-forecasting-icon-ch2" then
      println("   ✅ ICON-CH2 collection ID is correct")
    else
      println(s"   ❌ Incorrect collection ID: $iconCh2Collection")

    if meteoSwissApiUI.contains("meteoswiss.ch") then
      println("   ✅ API documentation URL points to MeteoSwiss")
    else
      println(s"   ❌ API documentation URL seems incorrect: $meteoSwissApiUI")

end MeteoSwissClientTest