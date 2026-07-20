package pme123.weather

import munit.*

class SempacherseeForecastTest extends munit.FunSuite:

  test("Bise detection (Guettingen-Genf >= +2 hPa)") {
    val data = createTestData(
      time = "2026-01-26T12:00",
      guettingenPress = 1015.0,
      genfPress = 1012.0 // Diff = +3 hPa
    )
    val forecast = SempacherseeForecastCalculator.calculateForecast(data._1, data._2, data._3)

    assert(forecast.nonEmpty, "Forecast should not be empty")
    assertEquals(forecast.head.windType, WindType.BiseGut, "Should detect Bise at +3 hPa")
    assert(forecast.head.forceKnots > 0, "Bise force should be positive")
  }

  test("Strong Bise detection (Guettingen-Genf >= +5 hPa)") {
    val data = createTestData(
      time = "2026-01-26T12:00",
      guettingenPress = 1018.0,
      genfPress = 1012.0 // Diff = +6 hPa
    )
    val forecast = SempacherseeForecastCalculator.calculateForecast(data._1, data._2, data._3)

    assert(forecast.nonEmpty, "Forecast should not be empty")
    assertEquals(forecast.head.windType, WindType.BiseStark, "Should detect strong Bise at +6 hPa")
  }

  test("Westwind detection (Guettingen-Genf <= -2 hPa)") {
    val data = createTestData(
      time = "2026-01-26T12:00",
      guettingenPress = 1010.0,
      genfPress = 1013.0 // Diff = -3 hPa
    )
    val forecast = SempacherseeForecastCalculator.calculateForecast(data._1, data._2, data._3)

    assert(forecast.nonEmpty, "Forecast should not be empty")
    assertEquals(forecast.head.windType, WindType.WestwindGut, "Should detect Westwind at -3 hPa")
  }

  test("Strong Westwind detection (Guettingen-Genf <= -5 hPa)") {
    val data = createTestData(
      time = "2026-01-26T12:00",
      guettingenPress = 1007.0,
      genfPress = 1013.0 // Diff = -6 hPa
    )
    val forecast = SempacherseeForecastCalculator.calculateForecast(data._1, data._2, data._3)

    assert(forecast.nonEmpty, "Forecast should not be empty")
    assertEquals(forecast.head.windType, WindType.WestwindStark, "Should detect strong Westwind at -6 hPa")
  }

  test("No dominant regime between -2 and +2 hPa") {
    val data = createTestData(
      time = "2026-01-26T12:00",
      guettingenPress = 1013.0,
      genfPress = 1012.0, // Diff = +1 hPa
      mosenWind = 3.0
    )
    val forecast = SempacherseeForecastCalculator.calculateForecast(data._1, data._2, data._3)

    assert(forecast.nonEmpty, "Forecast should not be empty")
    assertEquals(forecast.head.windType, WindType.Nothing, "Should show Nothing between -2 and +2 hPa")
  }

  test("Strong measured gust with a neutral pressure gradient is 'Böig', not 'Nothing'") {
    val data = createTestData(
      time = "2026-01-26T12:00",
      guettingenPress = 1013.0,
      genfPress = 1012.0, // Diff = +1 hPa - neither Bise nor Westwind
      mosenWind = 10.0,
      mosenGust = 45.0     // km/h - a real local gust despite the neutral gradient
    )
    val forecast = SempacherseeForecastCalculator.calculateForecast(data._1, data._2, data._3)

    assert(forecast.nonEmpty, "Forecast should not be empty")
    assertEquals(forecast.head.windType, WindType.Böig,
      "A strong gust without a Bise/Westwind signal should not be mislabeled 'Nothing'")
    assert(forecast.head.forceKnots > 20.0, "The real measured gust should still be reported, correctly converted from km/h")
  }

  test("Weak measured wind with a neutral pressure gradient stays 'Nothing'") {
    val data = createTestData(
      time = "2026-01-26T12:00",
      guettingenPress = 1013.0,
      genfPress = 1012.0, // Diff = +1 hPa
      mosenWind = 5.0,
      mosenGust = 10.0     // km/h - well below the Böig gust threshold
    )
    val forecast = SempacherseeForecastCalculator.calculateForecast(data._1, data._2, data._3)

    assert(forecast.nonEmpty, "Forecast should not be empty")
    assertEquals(forecast.head.windType, WindType.Nothing, "A mild gust shouldn't be flagged as 'Böig'")
  }

  test("Measured Mosen wind (km/h, correctly converted) floors the estimated force") {
    val data = createTestData(
      time = "2026-01-26T12:00",
      guettingenPress = 1015.0,
      genfPress = 1013.0, // Diff = +2 hPa (right at threshold, low pressure-based force)
      mosenWind = 40.0 // km/h - strong measured wind should dominate
    )
    val forecast = SempacherseeForecastCalculator.calculateForecast(data._1, data._2, data._3)

    assert(forecast.nonEmpty, "Forecast should not be empty")
    val expectedKnots = 40.0 * 0.539957
    assert(forecast.head.forceKnots >= expectedKnots - 0.01, "Measured Mosen wind should floor the force estimate, converted from km/h (not m/s)")
  }

  test("Hourly filtering (8-18h, every 2 hours)") {
    val multiHourData = 0.to(23).map { hour =>
      val time = f"2026-01-26T$hour%02d:00"
      createTestData(time = time)
    }

    val allGuettingen = multiHourData.flatMap(_._1)
    val allGenf       = multiHourData.flatMap(_._2)
    val allMosen      = multiHourData.flatMap(_._3)

    val forecast = SempacherseeForecastCalculator.calculateForecast(allGuettingen, allGenf, allMosen)

    assertEquals(forecast.length, 6, "Should have exactly 6 hourly forecasts")
    val expectedHours = Seq(8, 10, 12, 14, 16, 18)
    val actualHours = forecast.map(f => f.time.substring(11, 13).toInt)
    assertEquals(actualHours, expectedHours, "Should only include hours 8, 10, 12, 14, 16, 18")
  }

  // Helper method to create test data
  private def createTestData(
      time: String,
      guettingenPress: Double = 1013.0,
      genfPress: Double = 1013.0,
      mosenWind: Double = 3.0,
      mosenGust: Double = 8.0
  ): (Seq[HourlyDataSet], Seq[HourlyDataSet], Seq[HourlyDataSet]) =
    val guettingenData = Seq(HourlyDataSet(
      time = time,
      temperature_2m = 12.0,
      pressure_msl = guettingenPress,
      surface_pressure = guettingenPress - 2.0,
      wind_speed_10m = 4.0,
      wind_gusts_10m = 9.0,
      wind_direction_10m = 45.0,
      cloud_cover = 0.0
    ))

    val genfData = Seq(HourlyDataSet(
      time = time,
      temperature_2m = 14.0,
      pressure_msl = genfPress,
      surface_pressure = genfPress - 2.0,
      wind_speed_10m = 4.0,
      wind_gusts_10m = 9.0,
      wind_direction_10m = 225.0,
      cloud_cover = 0.0
    ))

    val mosenData = Seq(HourlyDataSet(
      time = time,
      temperature_2m = 13.0,
      pressure_msl = 1013.0,
      surface_pressure = 1011.0,
      wind_speed_10m = mosenWind,
      wind_gusts_10m = mosenGust,
      wind_direction_10m = 45.0,
      cloud_cover = 0.0
    ))

    (guettingenData, genfData, mosenData)
