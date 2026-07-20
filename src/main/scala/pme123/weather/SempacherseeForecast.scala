package pme123.weather

import scala.math.{abs, max}

/** Hourly analysis result used internally by the Sempachersee algorithm */
private case class SempacherseeHourlyAnalysis(
    time: String,
    windType: WindType,
    forceKnots: Double,
    guettingenGenfDiff: Double,
    mosenWindKnots: Double,
    mosenGustKnots: Double
)

/**
 * Forecast algorithm for Sempachersee.
 *
 * Unlike the Urnersee, the Sempachersee is a lowland lake with no valley/Föhn mechanics - its
 * dominant winds are purely synoptic (see [[pme123.weather.info.mittellandseen]] and esys.org's
 * "Revierinformation Sempachersee"):
 *   - Bise (NE, high-pressure driven): Güttingen pressure exceeds Genève - the same criterion
 *     already used for the "Mittellandseen" group label (`> 2 hPa`).
 *   - Südwest-/Westwind: the reverse gradient (`< -2 hPa`), driven by low-pressure systems
 *     passing north of the Alps ("Südwestlage"), most common in spring/early summer and autumn.
 *
 * Below +/-2 hPa neither regime dominates. On those days Sempachersee mostly depends on the
 * lake's own land/sea-breeze thermal circulation (auf-/ablandiger Wind), which this first
 * version does not model yet - that would need a lake-vs-hinterland temperature contrast (e.g.
 * Mosen vs. Egolzwil/Wynau) and is left for a later iteration.
 *
 * Force is estimated from how far the pressure gradient exceeds the threshold, floored by
 * Mosen's actually measured average wind speed (Mosen sits directly on the Sempachersee shore).
 * The knots-per-hPa scaling is an engineering approximation - no source quantifies it - kept
 * deliberately conservative since esys.org notes Sempachersee's Bise (at Nottwil) is weaker and
 * gustier than at other Mittelland lakes.
 *
 * Open-Meteo returns wind speed/gusts in km/h by default, converted via
 * [[UrnerseeForecastCommon.KmhToKnots]] - not the m/s-to-knots factor used elsewhere by mistake.
 */
object SempacherseeForecastCalculator:

  private val BiseThreshold = 2.0      // hPa - matches the existing Mittellandseen group criterion
  private val WestwindThreshold = -2.0 // hPa
  private val StrongThreshold = 5.0    // hPa beyond which a regime is considered "stark"
  private val BoeigThreshold = 20.0    // kn of GUST at Mosen - above this, despite a neutral pressure gradient,
                                        // it's not "no wind" - it's just not explained by Bise/Westwind (e.g. a
                                        // local squall/thunderstorm gust or the not-yet-modelled lake breeze).
                                        // Gusts (not average speed) are the relevant metric here since these
                                        // events are by nature short, gusty spikes rather than sustained wind.

  def calculateForecast(
      guettingenData: Seq[HourlyDataSet],
      genfData: Seq[HourlyDataSet],
      mosenData: Seq[HourlyDataSet]
  ): Seq[UrnerseeForecast] =
    // Align all data by time
    val times = guettingenData.map(_.time).toSet
      .intersect(genfData.map(_.time).toSet)
      .intersect(mosenData.map(_.time).toSet)
      .toSeq
      .sorted

    // Filter to hours 8-18, every 2 hours (8, 10, 12, 14, 16, 18)
    times
      .filter { time =>
        val hour = time.substring(11, 13).toInt
        hour >= 8 && hour <= 18 && hour % 2 == 0
      }
      .map { time =>
        val guettingen = guettingenData.find(_.time == time).get
        val genf = genfData.find(_.time == time).get
        val mosen = mosenData.find(_.time == time).get
        createHourlyForecast(analyzeHour(time, guettingen, genf, mosen))
      }

  private def analyzeHour(
      time: String,
      guettingen: HourlyDataSet,
      genf: HourlyDataSet,
      mosen: HourlyDataSet
  ): SempacherseeHourlyAnalysis =
    val diff = guettingen.pressure_msl - genf.pressure_msl
    val mosenWindKnots = max(mosen.wind_speed_10m * UrnerseeForecastCommon.KmhToKnots, 0.0)
    val mosenGustKnots = max(mosen.wind_gusts_10m * UrnerseeForecastCommon.KmhToKnots, 0.0)

    val (windType, forceKnots) =
      if diff >= BiseThreshold then
        val force = estimateForce(diff - BiseThreshold, mosenWindKnots)
        val windType = if diff >= StrongThreshold then WindType.BiseStark else WindType.BiseGut
        (windType, force)
      else if diff <= WestwindThreshold then
        val excess = abs(diff) - abs(WestwindThreshold)
        val force = estimateForce(excess, mosenWindKnots)
        val windType = if abs(diff) >= StrongThreshold then WindType.WestwindStark else WindType.WestwindGut
        (windType, force)
      else
        // Neutral pressure gradient - no Bise/Westwind. But if a real gust is still measured at
        // Mosen, don't silently call it "Nothing": show it as unclassified rather than
        // contradicting the displayed knots with a "no wind" label.
        val windType = if mosenGustKnots > BoeigThreshold then WindType.Böig else WindType.Nothing
        val force = if windType == WindType.Böig then mosenGustKnots else mosenWindKnots
        (windType, force)

    SempacherseeHourlyAnalysis(time, windType, forceKnots, diff, mosenWindKnots, mosenGustKnots)

  /** knots beyond the threshold, floored by the actually measured wind at Mosen */
  private def estimateForce(excessHpa: Double, mosenWindKnots: Double): Double =
    val pressureComponent = 6.0 + excessHpa * 2.0 // base 6 knots at the threshold, +2kn per extra hPa
    max(pressureComponent, mosenWindKnots)

  private def createHourlyForecast(analysis: SempacherseeHourlyAnalysis): UrnerseeForecast =
    UrnerseeForecast(
      time = analysis.time,
      windType = analysis.windType,
      forceKnots = analysis.forceKnots,
      info = generateHourlyInfo(analysis)
    )

  private def generateHourlyInfo(analysis: SempacherseeHourlyAnalysis): String =
    val regimeStatus = analysis.windType match
      case WindType.BiseStark     => "✓ Starke Bise (Nordost, Hochdruck-getrieben)"
      case WindType.BiseGut       => "✓ Bise (Nordost, Hochdruck-getrieben)"
      case WindType.WestwindStark => "✓ Starker Südwest-/Westwind (Tiefdrucklage)"
      case WindType.WestwindGut   => "✓ Südwest-/Westwind (Tiefdrucklage)"
      case WindType.Böig          => "⚠️ Wind gemessen, aber kein Bise-/Westwind-Regime erkennbar - vermutlich lokale Böe (Gewitter/Konvektion) oder See-Land-Thermik (noch nicht modelliert)"
      case _                      => "✗ Kein dominantes Regime, kaum Wind"

    val sections = Seq(
      s"<b>Regime:</b> $regimeStatus",
      f"<b>- Druckgradient Güttingen-Genf:</b> ${analysis.guettingenGenfDiff}%.1f hPa",
      s"<b>- Schwelle:</b> Bise ab +${BiseThreshold.toInt} hPa, Westwind ab ${WestwindThreshold.toInt} hPa, stark ab ±${StrongThreshold.toInt} hPa",
      f"<b>- Mittelwind Mosen:</b> ${analysis.mosenWindKnots}%.0f kn",
      f"<b>- Böen Mosen:</b> ${analysis.mosenGustKnots}%.0f kn"
    )

    sections.mkString("<br>")

end SempacherseeForecastCalculator
