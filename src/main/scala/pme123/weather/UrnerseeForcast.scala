package pme123.weather

import scala.math.{abs, max}
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Wind type for Urnersee forecast */
enum WindType:
  case Thermik      // Thermal wind
  case Föhnbise     // North wind (Föhn bise)
  case Föhn         // South Föhn
  case Nothing      // No significant wind

/** Forecast data for a specific hour */
case class UrnerseeForecast(
    time: String,           // Time in format "yyyy-MM-ddTHH:mm"
    windType: WindType,     // Wind type for this hour
    forceKnots: Double,     // Wind force in knots
    info: String            // Explanation of how the forecast was determined
)

/** Hourly analysis result used internally */
private case class HourlyAnalysis(
    time: String,
    windType: WindType,
    forceKnots: Double,
    luganoZurichDiff: Double,
    altdorfZurichDiff: Double,
    altdorfLucerneTempDiff: Double,
    altdorfGuetschTempDiff: Double,
    // Additional data for detailed info
    altdorfTemp: Double,
    lucerneTemp: Double,
    guetschTemp: Double,
    altdorfPressure: Double,
    luganoPressure: Double,
    zurichPressure: Double,
    dayAmplitude: Double
)

/** Forecast calculator for Urnersee based on weather station data */
object UrnerseeForecastCalculator:

  /**
   * Calculate daily forecast for Urnersee based on weather data
   *
   * @param altdorfData Weather data from Altdorf station
   * @param luganoData Weather data from Lugano station
   * @param zurichData Weather data from Zurich station
   * @param lucerneData Weather data from Lucerne station
   * @param guetschData Weather data from Gütsch station
   * @return Sequence of daily forecasts
   */
  def calculateForecast(
      altdorfData: Seq[HourlyDataSet],
      luganoData: Seq[HourlyDataSet],
      zurichData: Seq[HourlyDataSet],
      lucerneData: Seq[HourlyDataSet],
      guetschData: Seq[HourlyDataSet]
  ): Seq[UrnerseeForecast] =
    // Align all data by time
    val times = altdorfData.map(_.time).toSet
      .intersect(luganoData.map(_.time).toSet)
      .intersect(zurichData.map(_.time).toSet)
      .intersect(lucerneData.map(_.time).toSet)
      .intersect(guetschData.map(_.time).toSet)
      .toSeq
      .sorted

    // Calculate daily temperature amplitudes for Altdorf
    val dailyAmplitudes = altdorfData
      .groupBy(_.time.substring(0, 10)) // Group by date (yyyy-MM-dd)
      .map { case (date, dayData) =>
        val temps = dayData.map(_.temperature_2m)
        val amplitude = if (temps.nonEmpty) temps.max - temps.min else 0.0
        date -> amplitude
      }

    // Analyze each hour
    val hourlyAnalyses = times.map { time =>
      val altdorf = altdorfData.find(_.time == time).get
      val lugano = luganoData.find(_.time == time).get
      val zurich = zurichData.find(_.time == time).get
      val lucerne = lucerneData.find(_.time == time).get
      val guetsch = guetschData.find(_.time == time).get

      val date = time.substring(0, 10)
      val dayAmplitude = dailyAmplitudes.getOrElse(date, 0.0)

      analyzeHour(time, altdorf, lugano, zurich, lucerne, guetsch, dayAmplitude)
    }

    // Filter to hours 8-18, every 2 hours (8, 10, 12, 14, 16, 18)
    hourlyAnalyses
      .filter { analysis =>
        val hour = analysis.time.substring(11, 13).toInt
        hour >= 8 && hour <= 18 && hour % 2 == 0
      }
      .map(createHourlyForecast)

  /**
   * Analyze a single hour
   */
  private def analyzeHour(
      time: String,
      altdorf: HourlyDataSet,
      lugano: HourlyDataSet,
      zurich: HourlyDataSet,
      lucerne: HourlyDataSet,
      guetsch: HourlyDataSet,
      dayAmplitude: Double
  ): HourlyAnalysis =
    // Key pressure differences (in hPa)
    val luganoZurichDiff = lugano.pressure_msl - zurich.pressure_msl
    val altdorfZurichDiff = altdorf.pressure_msl - zurich.pressure_msl

    // Temperature differences for thermal analysis
    val altdorfLucerneTempDiff = altdorf.temperature_2m - lucerne.temperature_2m
    val altdorfGuetschTempDiff = altdorf.temperature_2m - guetsch.temperature_2m

    // Determine wind type and force
    val (windType, forceKnots) = determineWindConditions(
      luganoZurichDiff,
      altdorfZurichDiff,
      altdorfLucerneTempDiff,
      altdorfGuetschTempDiff,
      altdorf.wind_speed_10m,
      guetsch.wind_speed_10m
    )

    HourlyAnalysis(
      time,
      windType,
      forceKnots,
      luganoZurichDiff,
      altdorfZurichDiff,
      altdorfLucerneTempDiff,
      altdorfGuetschTempDiff,
      altdorf.temperature_2m,
      lucerne.temperature_2m,
      guetsch.temperature_2m,
      altdorf.pressure_msl,
      lugano.pressure_msl,
      zurich.pressure_msl,
      dayAmplitude
    )

  /**
   * Create hourly forecast from a single hour analysis
   */
  private def createHourlyForecast(analysis: HourlyAnalysis): UrnerseeForecast =
    val info = generateHourlyInfo(analysis)
    UrnerseeForecast(
      time = analysis.time,
      windType = analysis.windType,
      forceKnots = analysis.forceKnots,
      info = info
    )

  /**
   * Generate hourly info text for a single hour
   */
  private def generateHourlyInfo(analysis: HourlyAnalysis): String =
    val windTypeInfo = analysis.windType match {
      case WindType.Föhn => "Föhn"
      case WindType.Föhnbise => "Föhnbise"
      case WindType.Thermik => "Thermik"
      case WindType.Nothing => "Schwacher Wind"
    }

    // Föhn/Föhnbise Status
    val foehnStatus = if (analysis.luganoZurichDiff > 8.0 || analysis.altdorfZurichDiff > 0) {
      "✓ Föhn aktiv"
    } else if (analysis.luganoZurichDiff >= 6.0 && analysis.altdorfZurichDiff < 0) {
      "⚠️ Durchbruch möglich"
    } else if (analysis.luganoZurichDiff >= 2.0 && analysis.altdorfZurichDiff < 0) {
      "Föhnbise (Vakuum)"
    } else {
      "Keine Föhnbedingungen"
    }

    // Thermik Status
    val thermikStatus = if (analysis.altdorfLucerneTempDiff >= 5.0) "✓ Stark"
                        else if (analysis.altdorfLucerneTempDiff >= 3.0) "~ Moderat"
                        else "✗ Schwach"

    // Amplitude Status
    val amplitudeStatus = if (analysis.dayAmplitude >= 16.0) "✓ Hammer"
                          else if (analysis.dayAmplitude >= 10.0) "~ Gut"
                          else "✗ Schwach"

    // Schichtung Status
    val schichtungStatus = if (analysis.altdorfGuetschTempDiff >= 10.0) "✓ Labil"
                           else if (analysis.altdorfGuetschTempDiff >= 5.0) "~ Neutral"
                           else "✗ Inversion"

    // Kompakte Darstellung
    val sections = Seq(
      s"<b>$windTypeInfo - ${analysis.forceKnots.toInt} kn</b>",
      f"<b>Druckgradient:</b> Lug-Zrh ${analysis.luganoZurichDiff}%.1f / Alt-Zrh ${analysis.altdorfZurichDiff}%.1f hPa",
      s"<b>Föhn/Bise:</b> $foehnStatus",
      f"<b>Thermik Sog:</b> Alt-Luz ${analysis.altdorfLucerneTempDiff}%.1f°C $thermikStatus",
      f"<b>Tagesamplitude:</b> ${analysis.dayAmplitude}%.1f°C $amplitudeStatus",
      f"<b>Schichtung:</b> Alt ${analysis.altdorfTemp}%.1f°C / Gütsch ${analysis.guetschTemp}%.1f°C (Δ${analysis.altdorfGuetschTempDiff}%.1f°C) $schichtungStatus"
    )

    sections.mkString("<br>")

  /**
   * Determine wind conditions based on pressure and temperature differences
   *
   * Based on the theory from urnersee.scala:
   * - Föhn: Lugano-Zurich > +8 hPa OR Altdorf-Zurich becomes positive
   * - Föhnbise: Lugano-Zurich +2 to +8 hPa AND Altdorf-Zurich negative
   * - Thermik: Low pressure diff AND significant temp gradient (Altdorf-Lucerne > 5°C)
   * - Nothing: Low pressure diff AND low temp gradient
   */
  private def determineWindConditions(
      luganoZurichDiff: Double,
      altdorfZurichDiff: Double,
      altdorfLucerneTempDiff: Double,
      altdorfGuetschTempDiff: Double,
      altdorfWind: Double,
      guetschWind: Double
  ): (WindType, Double) =

    // Check for Föhn conditions (strong south wind)
    if (luganoZurichDiff > 8.0 || altdorfZurichDiff > 0) {
      val force = estimateFoehnForce(luganoZurichDiff, guetschWind)
      (WindType.Föhn, force)
    }
    // Check for Föhnbise conditions (north wind with Föhn aloft)
    else if (luganoZurichDiff >= 2.0 && luganoZurichDiff <= 8.0 && altdorfZurichDiff < 0) {
      val force = estimateFoehnbiseForce(abs(altdorfZurichDiff), altdorfWind)
      (WindType.Föhnbise, force)
    }
    // Check for Thermik conditions (thermal wind)
    else if (altdorfLucerneTempDiff >= 5.0 && altdorfGuetschTempDiff >= 10.0) {
      val force = estimateThermikForce(altdorfLucerneTempDiff, altdorfWind)
      (WindType.Thermik, force)
    }
    // Moderate thermal conditions
    else if (altdorfLucerneTempDiff >= 3.0 && altdorfGuetschTempDiff >= 5.0) {
      val force = estimateThermikForce(altdorfLucerneTempDiff, altdorfWind) * 0.6
      (WindType.Thermik, force)
    }
    // No significant wind
    else {
      (WindType.Nothing, max(altdorfWind * 1.94384, 0.0)) // Convert m/s to knots
    }

  /** Estimate Föhn force based on pressure gradient and mountain wind */
  private def estimateFoehnForce(luganoZurichDiff: Double, guetschWind: Double): Double =
    val pressureComponent = (luganoZurichDiff - 8.0) * 3.0 // 3 knots per hPa above threshold
    val windComponent = guetschWind * 1.94384 * 0.7 // 70% of mountain wind reaches valley
    max(15.0, pressureComponent + windComponent) // Minimum 15 knots for Föhn

  /** Estimate Föhnbise force based on vacuum effect */
  private def estimateFoehnbiseForce(altdorfZurichDiffAbs: Double, altdorfWind: Double): Double =
    val vacuumComponent = altdorfZurichDiffAbs * 2.5 // Stronger vacuum = stronger bise
    val measuredWind = altdorfWind * 1.94384
    max(8.0, max(vacuumComponent, measuredWind)) // Minimum 8 knots for Föhnbise

  /** Estimate Thermik force based on temperature gradient */
  private def estimateThermikForce(tempDiff: Double, altdorfWind: Double): Double =
    val thermalComponent = (tempDiff - 5.0) * 2.0 + 10.0 // Base 10 knots at 5°C diff
    val measuredWind = altdorfWind * 1.94384
    max(thermalComponent, measuredWind)

end UrnerseeForecastCalculator

