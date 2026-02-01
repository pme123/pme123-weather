package pme123.weather

import be.doeraene.webcomponents.ui5.*
import be.doeraene.webcomponents.ui5.configkeys.*
import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom

object WindSpeedExplanationDialog:

  def apply(): HtmlElement =
    val dialogOpenVar = Var(false)
    
    div(
      // Info Button
      Button(
        _.design := ButtonDesign.Transparent,
        _.icon := IconName.`hint`,
        _.tooltip := "Wie wird die Windgeschwindigkeit berechnet?",
        _.events.onClick --> { _ =>
          dialogOpenVar.set(true)
        }
      ),
      
      // Dialog
      Dialog(
        _.headerText := "Windgeschwindigkeits-Berechnung",
        _.open <-- dialogOpenVar.signal,
        
        // Dialog Content
        div(
          padding := "20px",
          maxHeight := "70vh",
          overflowY := "auto",
          
          // Einleitung
          p(
            marginBottom := "16px",
            "Die Windgeschwindigkeit wird je nach ", b("Windtyp"), " unterschiedlich berechnet. ",
            "Alle Werte werden in ", b("Knoten"), " angegeben (1 m/s = 1.94384 Knoten)."
          ),
          
          // Föhn Wind
          Panel(
            _.headerText := "1. Föhn Wind (Südwind)",
            _.collapsed := true,
            marginBottom := "12px",
            div(
              padding := "12px",
              p(
                marginBottom := "8px",
                b("Bedingung: "), "Lugano-Zürich Druckdifferenz > 8 hPa"
              ),
              p(
                marginBottom := "8px",
                b("Berechnung:")
              ),
              ul(
                marginLeft := "20px",
                marginBottom := "8px",
                li(b("Druckkomponente: "), "(Lugano-Zürich - 8.0) × 3.0 Knoten"),
                li(b("Windkomponente: "), "Gütsch-Wind × 1.94384 × 0.7 (70% erreicht das Tal)"),
                li(b("Minimum: "), "15 Knoten")
              ),
              p(
                marginBottom := "8px",
                b("Kategorien:")
              ),
              ul(
                marginLeft := "20px",
                li("≥ 25 Knoten: ", b("Föhn Sehr Stark"), " (Sturm)"),
                li("20-25 Knoten: ", b("Föhn Stark")),
                li("15-20 Knoten: ", b("Föhn Gut"))
              )
            )
          ),
          
          // Föhnbise Wind
          Panel(
            _.headerText := "2. Föhnbise Wind (Nordwind)",
            _.collapsed := true,
            marginBottom := "12px",
            div(
              padding := "12px",
              p(
                marginBottom := "8px",
                b("Bedingung: "), "Lugano-Zürich +2 bis +8 hPa UND Altdorf-Zürich negativ"
              ),
              p(
                marginBottom := "8px",
                b("Berechnung:")
              ),
              ul(
                marginLeft := "20px",
                marginBottom := "8px",
                li(b("Vakuumkomponente: "), "|Altdorf-Zürich| × 2.5 Knoten"),
                li(b("Gemessener Wind: "), "Altdorf-Wind × 1.94384"),
                li(b("Skalierung: "), "×1.3 (stark), ×1.1 (gut), ×0.9 (schwach)"),
                li(b("Minimum: "), "8 Knoten")
              ),
              p(
                marginBottom := "8px",
                b("Kategorien:")
              ),
              ul(
                marginLeft := "20px",
                li("+6 bis +8 hPa: ", b("Föhnbise Stark"), " - Durchbruch steht bevor"),
                li("+4 bis +6 hPa: ", b("Föhnbise Gut"), " - Durchbruch wahrscheinlich"),
                li("+2 bis +4 hPa: ", b("Föhnbise Schwach"), " - Föhn in der Höhe")
              )
            )
          ),
          
          // Thermik Wind
          Panel(
            _.headerText := "3. Thermik Wind (Temperaturgetrieben)",
            _.collapsed := true,
            marginBottom := "12px",
            div(
              padding := "12px",
              p(
                marginBottom := "8px",
                b("Bedingung: "), "Altdorf-Luzern Temperaturdifferenz ≥ 3°C"
              ),
              p(
                marginBottom := "8px",
                b("Berechnung:")
              ),
              ul(
                marginLeft := "20px",
                marginBottom := "8px",
                li(b("Basis: "), "10 Knoten bei 5°C Differenz"),
                li(b("Skalierung: "), "+2 Knoten pro °C über 5°C"),
                li(b("Tageszeit-Faktor: "), "0.5 (morgens) bis 1.0 (mittags)"),
                li(b("Saison-Faktor: "), "0.5 (Winter) bis 1.0 (Sommer)")
              ),
              p(
                marginBottom := "8px",
                b("Kategorien:")
              ),
              ul(
                marginLeft := "20px",
                li("≥ 13 Knoten: ", b("Thermik Sehr Gut")),
                li("9-13 Knoten: ", b("Thermik Gut")),
                li("< 9 Knoten: ", b("Thermik Schwach"))
              )
            )
          ),
          
          // Kein Wind
          MessageStrip(
            _.design := MessageStripDesign.Information,
            _.hideCloseButton := true,
            marginTop := "12px",
            b("Kein signifikanter Wind: "), 
            "Einfach der gemessene Altdorf-Wind in Knoten konvertiert."
          )
        ),
        
        // Dialog Footer
        _.slots.footer := div(
          display := "flex",
          justifyContent := "flex-end",
          padding := "8px",
          Button(
            _.design := ButtonDesign.Emphasized,
            _.events.onClick --> { _ =>
              dialogOpenVar.set(false)
            },
            "Schliessen"
          )
        )
      )
    )
  end apply

end WindSpeedExplanationDialog

