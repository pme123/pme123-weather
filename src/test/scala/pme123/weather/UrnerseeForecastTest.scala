package pme123.weather

import scala.math.abs
import munit.*

class UrnerseeForecastTest extends munit.FunSuite:

  test("Föhn detection with Lugano-Zurich > +8 hPa") {
    val foehnData = createTestData(
      time = "2026-01-26T12:00",
      luganoPress = 1020.0,
      zurichPress = 1010.0,  // Diff = +10 hPa (> +8)
      altdorfPress = 1012.0
    )
    val forecast = UrnerseeForecastCalculator.calculateForecast(
      foehnData._1, foehnData._2, foehnData._3, foehnData._4, foehnData._5
    )

    assert(forecast.nonEmpty, "Forecast should not be empty")
    assertEquals(forecast.head.windType, WindType.FöhnGut, "Should detect Föhn with Lugano-Zurich > +8 hPa")
    assert(forecast.head.forceKnots > 0, "Föhn force should be positive")
  }

  test("Föhn detection with Altdorf-Zurich positive (requires >= +4 hPa)") {
    val foehnData = createTestData(
      time = "2026-01-26T14:00",
      luganoPress = 1015.0,
      zurichPress = 1010.0,  // Diff = +5 hPa (>= +4)
      altdorfPress = 1011.0  // Diff = +1 hPa (positive)
    )
    val forecast = UrnerseeForecastCalculator.calculateForecast(
      foehnData._1, foehnData._2, foehnData._3, foehnData._4, foehnData._5
    )

    assert(forecast.nonEmpty, "Forecast should not be empty")
    assertEquals(forecast.head.windType, WindType.FöhnGut, "Should detect Föhn with Altdorf-Zurich positive and Lugano-Zurich >= +4 hPa")
    assert(forecast.head.forceKnots > 0, "Föhn force should be positive")
  }

  test("No Föhn below +4 hPa even with positive Altdorf-Zurich") {
    val foehnbiseData = createTestData(
      time = "2026-01-26T14:00",
      luganoPress = 1013.0,
      zurichPress = 1010.0,  // Diff = +3 hPa (< +4)
      altdorfPress = 1011.0  // Diff = +1 hPa (positive)
    )
    val forecast = UrnerseeForecastCalculator.calculateForecast(
      foehnbiseData._1, foehnbiseData._2, foehnbiseData._3, foehnbiseData._4, foehnbiseData._5
    )

    assert(forecast.nonEmpty, "Forecast should not be empty")
    assert(
      forecast.head.windType != WindType.FöhnGut &&
      forecast.head.windType != WindType.FöhnStark &&
      forecast.head.windType != WindType.FöhnSehrStark,
      "Should NOT detect any Föhn category below +4 hPa Lugano-Zurich"
    )
  }

  test("Weak Föhnbise detection (+2 to +4 hPa)") {
    val foehnbiseData = createTestData(
      time = "2026-01-26T10:00",
      luganoPress = 1013.0,
      zurichPress = 1010.0,  // Diff = +3 hPa (+2 to +4)
      altdorfPress = 1008.0  // Diff = -2 hPa (negative - vacuum effect)
    )
    val forecast = UrnerseeForecastCalculator.calculateForecast(
      foehnbiseData._1, foehnbiseData._2, foehnbiseData._3, foehnbiseData._4, foehnbiseData._5
    )

    assert(forecast.nonEmpty, "Forecast should not be empty")
    assertEquals(forecast.head.windType, WindType.FöhnbiseSchwach, "Should detect weak Föhnbise")
    assert(forecast.head.forceKnots > 0, "Föhnbise force should be positive")
  }

  test("Good Föhnbise detection (+4 to +6 hPa)") {
    val foehnbiseData = createTestData(
      time = "2026-01-26T10:00",
      luganoPress = 1015.0,
      zurichPress = 1010.0,  // Diff = +5 hPa (+4 to +6)
      altdorfPress = 1008.0  // Diff = -2 hPa (negative - vacuum effect)
    )
    val forecast = UrnerseeForecastCalculator.calculateForecast(
      foehnbiseData._1, foehnbiseData._2, foehnbiseData._3, foehnbiseData._4, foehnbiseData._5
    )

    assert(forecast.nonEmpty, "Forecast should not be empty")
    assertEquals(forecast.head.windType, WindType.FöhnbiseGut, "Should detect good Föhnbise")
    assert(forecast.head.forceKnots > 0, "Föhnbise force should be positive")
  }

  test("Strong Föhnbise detection (+6 to +8 hPa - breakthrough imminent)") {
    val foehnbiseData = createTestData(
      time = "2026-01-26T10:00",
      luganoPress = 1017.0,
      zurichPress = 1010.0,  // Diff = +7 hPa (+6 to +8)
      altdorfPress = 1008.0  // Diff = -2 hPa (negative - vacuum effect)
    )
    val forecast = UrnerseeForecastCalculator.calculateForecast(
      foehnbiseData._1, foehnbiseData._2, foehnbiseData._3, foehnbiseData._4, foehnbiseData._5
    )

    assert(forecast.nonEmpty, "Forecast should not be empty")
    assertEquals(forecast.head.windType, WindType.FöhnbiseStark, "Should detect strong Föhnbise")
    assert(forecast.head.info.contains("Durchbruch steht bevor"),
      "Should show Föhn breakthrough imminent warning for strong Föhnbise")
  }

  test("Very good Thermik detection (summer afternoon)") {
    val thermikData = createTestData(
      time = "2026-07-15T14:00",  // Summer afternoon - best conditions
      altdorfTemp = 25.0,
      lucerneTemp = 18.0,  // Diff = +7°C (≥ 5°C)
      guetschTemp = 12.0   // Diff = +13°C (≥ 10°C)
    )
    val forecast = UrnerseeForecastCalculator.calculateForecast(
      thermikData._1, thermikData._2, thermikData._3, thermikData._4, thermikData._5
    )

    assert(forecast.nonEmpty, "Forecast should not be empty")
    assertEquals(forecast.head.windType, WindType.ThermikSehrGut, "Should detect very good Thermik in summer afternoon")
    assert(forecast.head.forceKnots >= 13.0, "Very good Thermik force should be >= 13 knots")
  }

  test("Good Thermik detection (spring midday)") {
    val thermikData = createTestData(
      time = "2026-05-10T12:00",  // Spring midday - good conditions
      altdorfTemp = 20.0,
      lucerneTemp = 14.0,  // Diff = +6°C (≥ 5°C)
      guetschTemp = 8.0    // Diff = +12°C (≥ 10°C)
    )
    val forecast = UrnerseeForecastCalculator.calculateForecast(
      thermikData._1, thermikData._2, thermikData._3, thermikData._4, thermikData._5
    )

    assert(forecast.nonEmpty, "Forecast should not be empty")
    assert(
      forecast.head.windType == WindType.ThermikGut || forecast.head.windType == WindType.ThermikSehrGut,
      "Should detect good or very good Thermik in spring midday"
    )
    assert(forecast.head.forceKnots >= 9.0, "Good Thermik force should be >= 9 knots")
  }

  test("Weak Thermik detection (winter morning)") {
    val thermikData = createTestData(
      time = "2026-01-26T08:00",  // Winter morning - weak conditions
      altdorfTemp = 15.0,
      lucerneTemp = 11.0,  // Diff = +4°C (≥ 3°C)
      guetschTemp = 9.0    // Diff = +6°C (≥ 5°C)
    )
    val forecast = UrnerseeForecastCalculator.calculateForecast(
      thermikData._1, thermikData._2, thermikData._3, thermikData._4, thermikData._5
    )

    assert(forecast.nonEmpty, "Forecast should not be empty")
    assertEquals(forecast.head.windType, WindType.ThermikSchwach, "Should detect weak Thermik in winter morning")
    assert(forecast.head.forceKnots > 0, "Thermik force should be positive")
  }

  test("Nothing case for weak conditions") {
    val nothingData = createTestData(
      time = "2026-01-26T08:00",
      luganoPress = 1013.0,
      zurichPress = 1012.0,  // Diff = +1 hPa (< +2)
      altdorfPress = 1011.0, // Diff = -1 hPa
      altdorfTemp = 10.0,
      lucerneTemp = 9.0,     // Diff = +1°C (< 3°C)
      guetschTemp = 8.0      // Diff = +2°C (< 5°C)
    )
    val forecast = UrnerseeForecastCalculator.calculateForecast(
      nothingData._1, nothingData._2, nothingData._3, nothingData._4, nothingData._5
    )

    assert(forecast.nonEmpty, "Forecast should not be empty")
    assertEquals(forecast.head.windType, WindType.Nothing, "Should detect 'Nothing' for weak conditions")
  }

  test("Hourly filtering (8-18h, every 2 hours)") {
    val multiHourData = 0.to(23).map { hour =>
      val time = f"2026-01-26T$hour%02d:00"
      createTestData(time = time)
    }

    // Combine all hourly data
    val allAltdorf = multiHourData.flatMap(_._1)
    val allLugano = multiHourData.flatMap(_._2)
    val allZurich = multiHourData.flatMap(_._3)
    val allLucerne = multiHourData.flatMap(_._4)
    val allGuetsch = multiHourData.flatMap(_._5)

    val forecast = UrnerseeForecastCalculator.calculateForecast(
      allAltdorf, allLugano, allZurich, allLucerne, allGuetsch
    )

    // Should only have hours 8, 10, 12, 14, 16, 18 = 6 hours
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
      lucerneTemp: Double = 15.0,
      guetschTemp: Double = 10.0,
      altdorfWind: Double = 5.0,
      guetschWind: Double = 10.0
  ): (Seq[HourlyDataSet], Seq[HourlyDataSet], Seq[HourlyDataSet], Seq[HourlyDataSet], Seq[HourlyDataSet]) =
    val altdorfData = Seq(HourlyDataSet(
      time = time,
      temperature_2m = altdorfTemp,
      pressure_msl = altdorfPress,
      surface_pressure = altdorfPress - 2.0,
      wind_speed_10m = altdorfWind,
      wind_gusts_10m = altdorfWind + 5.0,
      wind_direction_10m = 180.0
    ))

    val luganoData = Seq(HourlyDataSet(
      time = time,
      temperature_2m = 18.0,
      pressure_msl = luganoPress,
      surface_pressure = luganoPress - 2.0,
      wind_speed_10m = 3.0,
      wind_gusts_10m = 8.0,
      wind_direction_10m = 180.0
    ))

    val zurichData = Seq(HourlyDataSet(
      time = time,
      temperature_2m = 12.0,
      pressure_msl = zurichPress,
      surface_pressure = zurichPress - 2.0,
      wind_speed_10m = 4.0,
      wind_gusts_10m = 9.0,
      wind_direction_10m = 270.0
    ))

    val lucerneData = Seq(HourlyDataSet(
      time = time,
      temperature_2m = lucerneTemp,
      pressure_msl = 1013.0,
      surface_pressure = 1011.0,
      wind_speed_10m = 3.5,
      wind_gusts_10m = 8.5,
      wind_direction_10m = 90.0
    ))

    val guetschData = Seq(HourlyDataSet(
      time = time,
      temperature_2m = guetschTemp,
      pressure_msl = 900.0,  // Higher altitude = lower pressure
      surface_pressure = 898.0,
      wind_speed_10m = guetschWind,
      wind_gusts_10m = guetschWind + 5.0,
      wind_direction_10m = 180.0
    ))

    (altdorfData, luganoData, zurichData, lucerneData, guetschData)

