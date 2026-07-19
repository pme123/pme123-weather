package pme123.weather

import scala.math.max

/** Hourly analysis result used internally by the Altdorf-Pilatus algorithm */
private case class PilatusHourlyAnalysis(
    time: String,
    windType: WindType,
    forceKnots: Double,
    luganoZurichDiff: Double,
    altdorfZurichDiff: Double,
    altdorfPilatusTempDiff: Double,
    guetschWindDirection: Double,
    swDamping: Boolean,
    altdorfCloudCover: Double,
    altdorfTemp: Double,
    pilatusTemp: Double
)

/**
 * Alternative Urnersee forecast algorithm focused on Thermik quality.
 *
 * Föhn/Föhnbise detection is identical to [[UrnerseeForecastCalculator]] (that part of the
 * theory is well established, see [[UrnerseeForecastCommon]]). Thermik detection instead
 * follows the criterion reported by local surfers/an SRF-Meteo expert (Surf-Forum.com,
 * thread "Urnersee Spot, Thermik etc"): the temperature difference between Altdorf and the
 * Pilatus summit predicts whether the thermal wind develops:
 *   - <= 10°C: Thermik fällt aus
 *   - >= 11°C: Thermik funktioniert
 *   - >= 12°C: Thermik funktioniert sehr gut
 *
 * The same source notes that southwesterly upper-level wind "destroys the Thermik tube" -
 * approximated here via the Gütsch wind direction, which dampens the force and downgrades
 * the classification by one step when it blows from the southwest (202.5°-247.5°).
 *
 * A large Altdorf-Pilatus gradient alone can't produce Thermik before the valley's boundary
 * layer has had time to build up (Talwind-Theorie, DHV "Talwind"): at 08:00 there is no
 * Thermik regardless of the temperature reading, and 09:00/10:00 is still a build-up phase -
 * see [[applyMorningGate]].
 *
 * Research (Surf-Forum.com) also stresses that Thermik needs "absolut klares Wetter" - even
 * light cloud cover disturbs its development, since it cuts the solar heating that drives the
 * circulation. The exact thresholds aren't quantified in that source, so this uses an
 * engineering approximation based on Altdorf's total cloud cover: above 20% the classification
 * is downgraded by one step (development disturbed), above 50% Thermik fails entirely
 * (not enough heating to start the circulation at all) - see [[CloudDisturbThreshold]] /
 * [[CloudFailThreshold]].
 */
object UrnerseeForecastCalculatorPilatus:

  private val ThermikFailsThreshold = 10.0
  private val ThermikWorksThreshold = 11.0
  private val ThermikVeryGoodThreshold = 12.0
  private val SwDirectionMin = 202.5
  private val SwDirectionMax = 247.5
  private val CloudDisturbThreshold = 20.0 // % cloud cover
  private val CloudFailThreshold = 50.0    // % cloud cover

  def calculateForecast(
      altdorfData: Seq[HourlyDataSet],
      pilatusData: Seq[HourlyDataSet],
      luganoData: Seq[HourlyDataSet],
      zurichData: Seq[HourlyDataSet],
      guetschData: Seq[HourlyDataSet]
  ): Seq[UrnerseeForecast] =
    // Align all data by time
    val times = altdorfData.map(_.time).toSet
      .intersect(pilatusData.map(_.time).toSet)
      .intersect(luganoData.map(_.time).toSet)
      .intersect(zurichData.map(_.time).toSet)
      .intersect(guetschData.map(_.time).toSet)
      .toSeq
      .sorted

    val hourlyAnalyses = times.map { time =>
      val altdorf = altdorfData.find(_.time == time).get
      val pilatus = pilatusData.find(_.time == time).get
      val lugano  = luganoData.find(_.time == time).get
      val zurich  = zurichData.find(_.time == time).get
      val guetsch = guetschData.find(_.time == time).get
      analyzeHour(time, altdorf, pilatus, lugano, zurich, guetsch)
    }

    // Filter to hours 8-18, every 2 hours (8, 10, 12, 14, 16, 18)
    hourlyAnalyses
      .filter { analysis =>
        val hour = analysis.time.substring(11, 13).toInt
        hour >= 8 && hour <= 18 && hour % 2 == 0
      }
      .map(createHourlyForecast)

  private def analyzeHour(
      time: String,
      altdorf: HourlyDataSet,
      pilatus: HourlyDataSet,
      lugano: HourlyDataSet,
      zurich: HourlyDataSet,
      guetsch: HourlyDataSet
  ): PilatusHourlyAnalysis =
    val luganoZurichDiff = lugano.pressure_msl - zurich.pressure_msl
    val altdorfZurichDiff = altdorf.pressure_msl - zurich.pressure_msl
    val altdorfPilatusTempDiff = altdorf.temperature_2m - pilatus.temperature_2m
    val guetschWindDirection = guetsch.wind_direction_10m
    val swDamping = guetschWindDirection >= SwDirectionMin && guetschWindDirection <= SwDirectionMax

    val (windType, forceKnots) = determineWindConditions(
      luganoZurichDiff,
      altdorfZurichDiff,
      altdorfPilatusTempDiff,
      altdorf.cloud_cover,
      altdorf.wind_speed_10m,
      guetsch.wind_speed_10m,
      swDamping,
      time
    )

    PilatusHourlyAnalysis(
      time,
      windType,
      forceKnots,
      luganoZurichDiff,
      altdorfZurichDiff,
      altdorfPilatusTempDiff,
      guetschWindDirection,
      swDamping,
      altdorf.cloud_cover,
      altdorf.temperature_2m,
      pilatus.temperature_2m
    )

  private def determineWindConditions(
      luganoZurichDiff: Double,
      altdorfZurichDiff: Double,
      altdorfPilatusTempDiff: Double,
      altdorfCloudCover: Double,
      altdorfWind: Double,
      guetschWind: Double,
      swDamping: Boolean,
      time: String
  ): (WindType, Double) =
    UrnerseeForecastCommon.determineFoehnConditions(luganoZurichDiff, altdorfZurichDiff, altdorfWind, guetschWind)
      .getOrElse {
        val measuredWindKnots = max(altdorfWind * 1.94384, 0.0)
        if (altdorfPilatusTempDiff <= ThermikFailsThreshold || altdorfCloudCover > CloudFailThreshold) {
          (WindType.Nothing, measuredWindKnots)
        } else {
          val baseWindType =
            if (altdorfPilatusTempDiff >= ThermikVeryGoodThreshold) WindType.ThermikSehrGut
            else if (altdorfPilatusTempDiff >= ThermikWorksThreshold) WindType.ThermikGut
            else WindType.ThermikSchwach // 10-11°C: grey zone, borderline

          val afterSwDamping = if (swDamping) downgrade(baseWindType) else baseWindType
          val cloudDamping = altdorfCloudCover > CloudDisturbThreshold
          val dampedWindType = if (cloudDamping) downgrade(afterSwDamping) else afterSwDamping

          val baseForce = estimateThermikForce(altdorfPilatusTempDiff, altdorfWind)
          val swForce = if (swDamping) baseForce * 0.5 else baseForce // SW upper wind destroys the Thermik tube
          val cloudForce = if (cloudDamping) swForce * 0.7 else swForce // clouds cut the solar heating that drives it
          val adjustedForce = UrnerseeForecastCommon.applyThermikTimeFactors(cloudForce, time)

          val hour = time.substring(11, 13).toInt
          applyMorningGate(dampedWindType, adjustedForce, measuredWindKnots, hour)
        }
      }

  /**
   * Even a large Altdorf-Pilatus gradient can't produce Thermik before the valley's boundary
   * layer has had time to build up (Talwind-Theorie: reliable Thermik only sets in from ca.
   * 11:00 - see DHV "Talwind" article). At 08:00 there simply is no thermal wind yet, no matter
   * what the instantaneous temperature reading suggests; at 09:00/10:00 it is still forming.
   */
  private def applyMorningGate(windType: WindType, forceKnots: Double, measuredWindKnots: Double, hour: Int): (WindType, Double) =
    if (hour <= 8) (WindType.Nothing, measuredWindKnots)
    else if (hour <= 10) (capSeverity(windType, WindType.ThermikSchwach), forceKnots)
    else (windType, forceKnots)

  private def thermikSeverity(windType: WindType): Int = windType match
    case WindType.Nothing        => 0
    case WindType.ThermikSchwach => 1
    case WindType.ThermikGut     => 2
    case WindType.ThermikSehrGut => 3
    case _                       => 3

  private def capSeverity(windType: WindType, cap: WindType): WindType =
    if (thermikSeverity(windType) > thermikSeverity(cap)) cap else windType

  private def downgrade(windType: WindType): WindType = windType match
    case WindType.ThermikSehrGut => WindType.ThermikGut
    case WindType.ThermikGut     => WindType.ThermikSchwach
    case other                   => other

  /** Estimate Thermik force based on the Altdorf-Pilatus temperature gradient */
  private def estimateThermikForce(tempDiff: Double, altdorfWind: Double): Double =
    val thermalComponent = (tempDiff - ThermikFailsThreshold) * 3.0 + 8.0 // Base 8 knots right above the fail threshold
    val measuredWind = altdorfWind * 1.94384
    max(thermalComponent, measuredWind)

  private def createHourlyForecast(analysis: PilatusHourlyAnalysis): UrnerseeForecast =
    UrnerseeForecast(
      time = analysis.time,
      windType = analysis.windType,
      forceKnots = analysis.forceKnots,
      info = generateHourlyInfo(analysis)
    )

  private def generateHourlyInfo(analysis: PilatusHourlyAnalysis): String =
    val foehnStatus = UrnerseeForecastCommon.foehnStatusText(analysis.luganoZurichDiff, analysis.altdorfZurichDiff)

    val thermikTriggerStatus =
      if (analysis.altdorfPilatusTempDiff >= ThermikVeryGoodThreshold) "✓ Sehr gut (SRF-Kriterium: ≥12°C)"
      else if (analysis.altdorfPilatusTempDiff >= ThermikWorksThreshold) "✓ Funktioniert (SRF-Kriterium: ≥11°C)"
      else if (analysis.altdorfPilatusTempDiff > ThermikFailsThreshold) "~ Grenzwertig (10-11°C)"
      else "✗ Fällt aus (SRF-Kriterium: ≤10°C)"

    val degreeStr = f"${analysis.guetschWindDirection}%.0f°"
    val hoehenwindStatus =
      if (analysis.swDamping) s"⚠️ SW-Dämpfung aktiv ($degreeStr, zerstört laut Theorie den Thermikschlauch)"
      else s"ok ($degreeStr, keine SW-Störung)"

    val wolkenStatus =
      if (analysis.altdorfCloudCover > CloudFailThreshold) "✗ Zu stark bewölkt - zu wenig Sonneneinstrahlung für Thermik"
      else if (analysis.altdorfCloudCover > CloudDisturbThreshold) "⚠️ Störung - Entwicklung durch Bewölkung gebremst"
      else "✓ Klar genug"

    val hour = analysis.time.substring(11, 13).toInt
    val tageszeitStatus =
      if (hour <= 8) "✗ Zu früh - Talwind/Grenzschicht noch nicht aufgebaut, keine Thermik möglich"
      else if (hour <= 10) "~ Aufbauphase - zuverlässige Thermik setzt meist erst ab ca. 11 Uhr ein"
      else "✓ Thermik-Zeitfenster"

    val sections = Seq(
      s"<b>Föhn:</b> $foehnStatus",
      f"<b>- Druckgradient:</b> Lug-Zrh ${analysis.luganoZurichDiff}%.1f / Alt-Zrh ${analysis.altdorfZurichDiff}%.1f hPa",
      "<b>Thermik (Altdorf-Pilatus-Kriterium)</b>",
      f"<b>- Trigger:</b> Alt ${analysis.altdorfTemp}%.1f°C / Pilatus ${analysis.pilatusTemp}%.1f°C (Δ${analysis.altdorfPilatusTempDiff}%.1f°C) <b>$thermikTriggerStatus</b>",
      s"<b>- Höhenwind Gütsch:</b> $hoehenwindStatus",
      f"<b>- Bewölkung Altdorf:</b> ${analysis.altdorfCloudCover}%.0f%% <b>$wolkenStatus</b>",
      f"<b>- Tageszeit:</b> $hour%02d:00 Uhr <b>$tageszeitStatus</b>"
    )

    sections.mkString("<br>")

end UrnerseeForecastCalculatorPilatus
