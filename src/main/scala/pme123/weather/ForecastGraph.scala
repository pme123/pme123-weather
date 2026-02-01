package pme123.weather

import com.raquo.laminar.api.L.*
import plotly.*
import plotly.Plotly.*
import plotly.element.*
import plotly.layout.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

// Facade for Plotly.js relayout function
@js.native
@JSGlobal("Plotly")
object PlotlyJS extends js.Object:
  def relayout(divId: String, update: js.Object): js.Promise[js.Any] = js.native

object ForecastGraph:

  def apply(
      groupId: String,
      forecasts: Seq[UrnerseeForecast]
  ): Unit =
    WeatherLogger.debug(s"ForecastGraph: $groupId with ${forecasts.size} hours")

    if (forecasts.isEmpty) {
      WeatherLogger.warn(s"No forecast data for $groupId")
      return ()
    }

    // Extract data for plotting
    val times = forecasts.map(_.time)
    val forces = forecasts.map(_.forceKnots)
    val windTypes = forecasts.map(_.windType)
    val infos = forecasts.map(_.info)
    
    // Format times for display
    val timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
    val displayFormatter = DateTimeFormatter.ofPattern("EEE HH:mm")
    val displayTimes = times.map { time =>
      val dateTime = java.time.LocalDateTime.parse(time, timeFormatter)
      dateTime.format(displayFormatter)
    }

    // Create custom tick labels - only show date for first bar of each day
    val dateFormatter = DateTimeFormatter.ofPattern("EEE dd.MM")
    val tickLabels = times.zipWithIndex.map { case (time, idx) =>
      val dateTime = java.time.LocalDateTime.parse(time, timeFormatter)

      // Check if this is the first bar of a new day
      val isFirstOfDay = if (idx == 0) {
        true
      } else {
        val prevDateTime = java.time.LocalDateTime.parse(times(idx - 1), timeFormatter)
        dateTime.toLocalDate != prevDateTime.toLocalDate
      }

      if (isFirstOfDay) {
        dateTime.format(dateFormatter) // Show "Mon 26.01"
      } else {
        "" // Show nothing for other bars
      }
    }
    
    // Color mapping for wind types
    def getColor(windType: WindType): String = windType match {
      case WindType.Föhn => "rgba(211, 47, 47, 0.85)"          // Dark Red (strong Föhn)
      case WindType.FöhnbiseStark => "rgba(25, 118, 210, 0.85)"   // Dark Blue (breakthrough imminent)
      case WindType.FöhnbiseGut => "rgba(66, 165, 245, 0.8)"      // Medium Blue (high probability)
      case WindType.FöhnbiseSchwach => "rgba(144, 202, 249, 0.75)" // Light Blue (moderate)
      case WindType.Thermik => "rgba(76, 175, 80, 0.8)"        // Green
      case WindType.Nothing => "rgba(158, 158, 158, 0.6)"      // Gray
    }
    
    // Create bar chart with color-coded wind types
    val bars = windTypes.zipWithIndex.map { case (windType, idx) =>
      Bar(
        Seq(displayTimes(idx)),
        Seq(forces(idx))
      )
        .withName(windType.toString)
        .withMarker(Marker()
          .withColor(Color.StringColor(getColor(windType))))
          .withHovertemplate(
            s"""<b>${displayTimes(idx)}</b><br>
               |<b>${windType}</b><br>
               |Windstärke: ${forces(idx).toInt} kn<br>
               |<br>${infos(idx)}
               |<extra></extra>
               |""".stripMargin
        )
        .withShowlegend(false)
    }
    
    // Create legend items (one per wind type, not per bar)
    val legendItems = WindType.values.toSeq.map { windType =>
      Scatter(
        Seq.empty[String],
        Seq.empty[Double]
      )
        .withName(windType.toString)
        .withMode(ScatterMode(ScatterMode.Markers))
        .withMarker(Marker()
          .withSize(12)
          .withColor(Color.StringColor(getColor(windType))))
        .withShowlegend(true)
    }
    
    val allTraces = bars ++ legendItems
    
    val layout = Layout()
      .withTitle(s"Prognose Urnersee - 8-18h (alle 2 Stunden)")
      .withXaxis(Axis()
        .withTitle("Zeit")
        .withTickangle(-45)
        .withTicktext(tickLabels)
        .withTickvals(displayTimes)
        .withGridcolor(Color.StringColor("rgba(0, 0, 0, 0.1)"))
        .withShowgrid(true))
      .withYaxis(Axis()
        .withTitle("Windstärke (Knoten)")
        .withGridcolor(Color.StringColor("rgba(0, 0, 0, 0.1)"))
        .withShowgrid(true))
      .withPaper_bgcolor(Color.StringColor("rgba(0, 0, 0, 0)"))
      .withPlot_bgcolor(Color.StringColor("rgba(255, 255, 255, 0.9)"))
      .withShowlegend(true)
      .withAutosize(true)
      .withMargin(Margin().withL(60).withR(30).withT(60).withB(120))
      .withLegend(Legend()
        .withX(0.5)
        .withY(-0.25)
        .withBgcolor(Color.StringColor("rgba(255, 255, 255, 0.95)"))
        .withBordercolor(Color.StringColor("rgba(0, 0, 0, 0.2)"))
        .withOrientation(Orientation.Horizontal)
        .withXanchor(Anchor.Center)
        .withYanchor(Anchor.Top)
        .withFont(Font()
          .withSize(13)
          .withColor(Color.StringColor("rgba(0, 0, 0, 1.0)"))))
      .withHovermode(HoverMode.Closest)
      .withBarmode(BarMode.Overlay)
    
    allTraces.plot(s"forecast-$groupId", layout)

    // Force left alignment of tooltip text using Plotly.js relayout
    val divId = s"forecast-$groupId"
    val update = js.Dynamic.literal("hoverlabel.align" -> "left")
    PlotlyJS.relayout(divId, update.asInstanceOf[js.Object])

  end apply

end ForecastGraph

