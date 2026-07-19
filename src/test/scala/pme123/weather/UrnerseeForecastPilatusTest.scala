package pme123.weather

import munit.*

class UrnerseeForecastPilatusTest extends munit.FunSuite:

  test("Föhn detection matches the Standard algorithm (Lugano-Zurich > +8 hPa)") {
    val data = createTestData(
      time = "2026-01-26T12:00",
      luganoPress = 1020.0,
      zurichPress = 1010.0,  // Diff = +10 hPa (> +8)
      altdorfPress = 1012.0
    )
    val forecast = UrnerseeForecastCalculatorPilatus.calculateForecast(
      data._1, data._2, data._3, data._4, data._5
    )

    assert(forecast.nonEmpty, "Forecast should not be empty")
    assertEquals(forecast.head.windType, WindType.FöhnGut, "Should detect Föhn with Lugano-Zurich > +8 hPa")
  }

  test("Thermik fails at Altdorf-Pilatus diff <= 10°C") {
    val data = createTestData(
      time = "2026-07-15T14:00",
      altdorfTemp = 20.0,
      pilatusTemp = 10.0 // Diff = 10°C (<= 10 -> fails)
    )
    val forecast = UrnerseeForecastCalculatorPilatus.calculateForecast(
      data._1, data._2, data._3, data._4, data._5
    )

    assert(forecast.nonEmpty, "Forecast should not be empty")
    assertEquals(forecast.head.windType, WindType.Nothing, "Thermik should fail at exactly 10°C diff")
  }

  test("Thermik works at Altdorf-Pilatus diff >= 11°C") {
    val data = createTestData(
      time = "2026-07-15T14:00",
      altdorfTemp = 21.0,
      pilatusTemp = 10.0 // Diff = 11°C
    )
    val forecast = UrnerseeForecastCalculatorPilatus.calculateForecast(
      data._1, data._2, data._3, data._4, data._5
    )

    assert(forecast.nonEmpty, "Forecast should not be empty")
    assertEquals(forecast.head.windType, WindType.ThermikGut, "Thermik should work at 11°C diff")
    assert(forecast.head.forceKnots > 0, "Thermik force should be positive")
  }

  test("Thermik very good at Altdorf-Pilatus diff >= 12°C") {
    val data = createTestData(
      time = "2026-07-15T14:00",
      altdorfTemp = 22.0,
      pilatusTemp = 10.0 // Diff = 12°C
    )
    val forecast = UrnerseeForecastCalculatorPilatus.calculateForecast(
      data._1, data._2, data._3, data._4, data._5
    )

    assert(forecast.nonEmpty, "Forecast should not be empty")
    assertEquals(forecast.head.windType, WindType.ThermikSehrGut, "Thermik should be very good at 12°C diff")
  }

  test("Southwest upper wind at Gütsch downgrades an otherwise very good Thermik") {
    val withoutDamping = createTestData(
      time = "2026-07-15T14:00",
      altdorfTemp = 22.0,
      pilatusTemp = 10.0, // Diff = 12°C -> ThermikSehrGut
      guetschWindDirection = 0.0
    )
    val withDamping = createTestData(
      time = "2026-07-15T14:00",
      altdorfTemp = 22.0,
      pilatusTemp = 10.0, // Diff = 12°C -> would be ThermikSehrGut
      guetschWindDirection = 225.0 // Southwest
    )

    val forecastWithout = UrnerseeForecastCalculatorPilatus.calculateForecast(
      withoutDamping._1, withoutDamping._2, withoutDamping._3, withoutDamping._4, withoutDamping._5
    )
    val forecastWith = UrnerseeForecastCalculatorPilatus.calculateForecast(
      withDamping._1, withDamping._2, withDamping._3, withDamping._4, withDamping._5
    )

    assertEquals(forecastWithout.head.windType, WindType.ThermikSehrGut, "Sanity check: no damping should be very good")
    assertEquals(forecastWith.head.windType, WindType.ThermikGut, "SW upper wind should downgrade the classification by one step")
    assert(forecastWith.head.forceKnots < forecastWithout.head.forceKnots, "SW upper wind should reduce the force")
  }

  test("No Thermik at 08:00 even with an excellent Altdorf-Pilatus gradient") {
    val data = createTestData(
      time = "2026-07-15T08:00",
      altdorfTemp = 22.0,
      pilatusTemp = 10.0 // Diff = 12°C -> would be ThermikSehrGut later in the day
    )
    val forecast = UrnerseeForecastCalculatorPilatus.calculateForecast(
      data._1, data._2, data._3, data._4, data._5
    )

    assert(forecast.nonEmpty, "Forecast should not be empty")
    assertEquals(forecast.head.windType, WindType.Nothing,
      "The valley's boundary layer hasn't built up yet at 08:00, regardless of the temperature reading")
  }

  test("Thermik is capped at 'Schwach' during the 09:00-10:00 build-up phase") {
    val data = createTestData(
      time = "2026-07-15T10:00",
      altdorfTemp = 22.0,
      pilatusTemp = 10.0 // Diff = 12°C -> would be ThermikSehrGut from 11:00 on
    )
    val forecast = UrnerseeForecastCalculatorPilatus.calculateForecast(
      data._1, data._2, data._3, data._4, data._5
    )

    assert(forecast.nonEmpty, "Forecast should not be empty")
    assertEquals(forecast.head.windType, WindType.ThermikSchwach,
      "Thermik is still building up at 10:00 and should be capped, even with a 12°C gradient")
  }

  test("Heavy cloud cover (> 50%) prevents Thermik entirely, even with an excellent gradient") {
    val data = createTestData(
      time = "2026-07-15T14:00",
      altdorfTemp = 22.0,
      pilatusTemp = 10.0, // Diff = 12°C -> would be ThermikSehrGut if clear
      altdorfCloudCover = 60.0
    )
    val forecast = UrnerseeForecastCalculatorPilatus.calculateForecast(
      data._1, data._2, data._3, data._4, data._5
    )

    assert(forecast.nonEmpty, "Forecast should not be empty")
    assertEquals(forecast.head.windType, WindType.Nothing,
      "Too little solar heating to start the thermal circulation above 50% cloud cover")
  }

  test("Moderate cloud cover (20-50%) downgrades the classification by one step") {
    val clear = createTestData(
      time = "2026-07-15T14:00",
      altdorfTemp = 22.0,
      pilatusTemp = 10.0, // Diff = 12°C -> ThermikSehrGut
      altdorfCloudCover = 0.0
    )
    val cloudy = createTestData(
      time = "2026-07-15T14:00",
      altdorfTemp = 22.0,
      pilatusTemp = 10.0,
      altdorfCloudCover = 35.0
    )

    val forecastClear = UrnerseeForecastCalculatorPilatus.calculateForecast(
      clear._1, clear._2, clear._3, clear._4, clear._5
    )
    val forecastCloudy = UrnerseeForecastCalculatorPilatus.calculateForecast(
      cloudy._1, cloudy._2, cloudy._3, cloudy._4, cloudy._5
    )

    assertEquals(forecastClear.head.windType, WindType.ThermikSehrGut, "Sanity check: clear sky should be very good")
    assertEquals(forecastCloudy.head.windType, WindType.ThermikGut, "Moderate cloud cover should downgrade by one step")
    assert(forecastCloudy.head.forceKnots < forecastClear.head.forceKnots, "Moderate cloud cover should reduce the force")
  }

  test("Hourly filtering (8-18h, every 2 hours)") {
    val multiHourData = 0.to(23).map { hour =>
      val time = f"2026-01-26T$hour%02d:00"
      createTestData(time = time)
    }

    val allAltdorf = multiHourData.flatMap(_._1)
    val allPilatus = multiHourData.flatMap(_._2)
    val allLugano  = multiHourData.flatMap(_._3)
    val allZurich  = multiHourData.flatMap(_._4)
    val allGuetsch = multiHourData.flatMap(_._5)

    val forecast = UrnerseeForecastCalculatorPilatus.calculateForecast(
      allAltdorf, allPilatus, allLugano, allZurich, allGuetsch
    )

    assertEquals(forecast.length, 6, "Should have exactly 6 hourly forecasts")
    val expectedHours = Seq(8, 10, 12, 14, 16, 18)
    val actualHours = forecast.map(f => f.time.substring(11, 13).toInt)
    assertEquals(actualHours, expectedHours, "Should only include hours 8, 10, 12, 14, 16, 18")
  }

  // Helper method to create test data
  private def createTestData(
      time: String,
      luganoPress: Double = 1013.0,
      zurichPress: Double = 1013.0,
      altdorfPress: Double = 1013.0,
      altdorfTemp: Double = 15.0,
      pilatusTemp: Double = 15.0,
      altdorfWind: Double = 5.0,
      guetschWind: Double = 10.0,
      guetschWindDirection: Double = 180.0,
      altdorfCloudCover: Double = 0.0
  ): (Seq[HourlyDataSet], Seq[HourlyDataSet], Seq[HourlyDataSet], Seq[HourlyDataSet], Seq[HourlyDataSet]) =
    val altdorfData = Seq(HourlyDataSet(
      time = time,
      temperature_2m = altdorfTemp,
      pressure_msl = altdorfPress,
      surface_pressure = altdorfPress - 2.0,
      wind_speed_10m = altdorfWind,
      wind_gusts_10m = altdorfWind + 5.0,
      wind_direction_10m = 180.0,
      cloud_cover = altdorfCloudCover
    ))

    val pilatusData = Seq(HourlyDataSet(
      time = time,
      temperature_2m = pilatusTemp,
      pressure_msl = 800.0, // High altitude summit = low pressure
      surface_pressure = 798.0,
      wind_speed_10m = 8.0,
      wind_gusts_10m = 15.0,
      wind_direction_10m = 200.0,
      cloud_cover = 0.0
    ))

    val luganoData = Seq(HourlyDataSet(
      time = time,
      temperature_2m = 18.0,
      pressure_msl = luganoPress,
      surface_pressure = luganoPress - 2.0,
      wind_speed_10m = 3.0,
      wind_gusts_10m = 8.0,
      wind_direction_10m = 180.0,
      cloud_cover = 0.0
    ))

    val zurichData = Seq(HourlyDataSet(
      time = time,
      temperature_2m = 12.0,
      pressure_msl = zurichPress,
      surface_pressure = zurichPress - 2.0,
      wind_speed_10m = 4.0,
      wind_gusts_10m = 9.0,
      wind_direction_10m = 270.0,
      cloud_cover = 0.0
    ))

    val guetschData = Seq(HourlyDataSet(
      time = time,
      temperature_2m = 10.0,
      pressure_msl = 900.0,
      surface_pressure = 898.0,
      wind_speed_10m = guetschWind,
      wind_gusts_10m = guetschWind + 5.0,
      wind_direction_10m = guetschWindDirection,
      cloud_cover = 0.0
    ))

    (altdorfData, pilatusData, luganoData, zurichData, guetschData)
