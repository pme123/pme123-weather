package pme123.weather

import com.raquo.laminar.api.L.{*, given}

object WindSpeedExplanationDialog:

  def apply(): HtmlElement =
    val openVar = Var(false)

    div(
      // ── Info icon ──────────────────────────────────────────────────────────
      span(
        className := "info-icon",
        "i",
        onClick --> { _ => openVar.set(true) }
      ),

      // ── Modal (rendered only when open) ───────────────────────────────────
      child <-- openVar.signal.map:
        case false => emptyNode
        case true =>
          div(
            className := "info-modal-overlay",
            onClick --> { _ => openVar.set(false) },

            div(
              className := "info-modal",
              onClick --> { ev => ev.stopPropagation() },

              // Header
              div(
                className := "info-modal-header",
                span(className := "info-modal-title", "Windgeschwindigkeits-Berechnung"),
                button(
                  className := "info-modal-close",
                  "✕",
                  onClick --> { _ => openVar.set(false) }
                )
              ),

              // Body
              div(
                className := "info-modal-body",

                p(
                  className := "info-p",
                  marginBottom := "14px",
                  "Die Windgeschwindigkeit wird je nach ", b("Windtyp"), " unterschiedlich berechnet. ",
                  "Alle Werte in ", b("Knoten"), " (1 m/s = 1.94384 kn)."
                ),

                detailsTag(
                  className := "info-group",
                  summaryTag(className := "info-group-title", "1. Föhn Wind (Südwind)"),
                  div(
                    className := "info-sub",
                    p(className := "info-p", b("Bedingung: "), "Lugano-Zürich Druckdifferenz > 8 hPa"),
                    p(className := "info-p", b("Berechnung:")),
                    ul(className := "info-ul",
                      li(b("Druckkomponente: "), "(Lugano-Zürich - 8.0) × 3.0 kn"),
                      li(b("Windkomponente: "), "Gütsch-Wind × 1.94384 × 0.7 (70% erreicht das Tal)"),
                      li(b("Minimum: "), "15 kn")
                    ),
                    p(className := "info-p", b("Kategorien:")),
                    ul(className := "info-ul",
                      li("≥ 25 kn: ", b("Föhn Sehr Stark"), " (Sturm)"),
                      li("20–25 kn: ", b("Föhn Stark")),
                      li("15–20 kn: ", b("Föhn Gut"))
                    )
                  )
                ),

                detailsTag(
                  className := "info-group",
                  summaryTag(className := "info-group-title", "2. Föhnbise Wind (Nordwind)"),
                  div(
                    className := "info-sub",
                    p(className := "info-p", b("Bedingung: "), "Lugano-Zürich +2 bis +8 hPa UND Altdorf-Zürich negativ"),
                    p(className := "info-p", b("Berechnung:")),
                    ul(className := "info-ul",
                      li(b("Vakuumkomponente: "), "|Altdorf-Zürich| × 2.5 kn"),
                      li(b("Gemessener Wind: "), "Altdorf-Wind × 1.94384"),
                      li(b("Skalierung: "), "×1.3 (stark), ×1.1 (gut), ×0.9 (schwach)"),
                      li(b("Minimum: "), "8 kn")
                    ),
                    p(className := "info-p", b("Kategorien:")),
                    ul(className := "info-ul",
                      li("+6 bis +8 hPa: ", b("Föhnbise Stark"), " – Durchbruch steht bevor"),
                      li("+4 bis +6 hPa: ", b("Föhnbise Gut"), " – Durchbruch wahrscheinlich"),
                      li("+2 bis +4 hPa: ", b("Föhnbise Schwach"), " – Föhn in der Höhe")
                    )
                  )
                ),

                detailsTag(
                  className := "info-group",
                  summaryTag(className := "info-group-title", "3. Thermik Wind (Temperaturgetrieben)"),
                  div(
                    className := "info-sub",
                    p(className := "info-p", b("Bedingung: "), "Altdorf-Luzern Temperaturdifferenz ≥ 3°C"),
                    p(className := "info-p", b("Berechnung:")),
                    ul(className := "info-ul",
                      li(b("Basis: "), "10 kn bei 5°C Differenz"),
                      li(b("Skalierung: "), "+2 kn pro °C über 5°C"),
                      li(b("Tageszeit-Faktor: "), "0.5 (morgens) bis 1.0 (mittags)"),
                      li(b("Saison-Faktor: "), "0.5 (Winter) bis 1.0 (Sommer)")
                    ),
                    p(className := "info-p", b("Kategorien:")),
                    ul(className := "info-ul",
                      li("≥ 13 kn: ", b("Thermik Sehr Gut")),
                      li("9–13 kn: ", b("Thermik Gut")),
                      li("< 9 kn: ", b("Thermik Schwach"))
                    )
                  )
                ),

                div(
                  className := "info-msg info-msg-info",
                  marginTop := "12px",
                  b("Kein signifikanter Wind: "),
                  "Einfach der gemessene Altdorf-Wind in Knoten konvertiert."
                )
              )
            )
          )
    )
  end apply

end WindSpeedExplanationDialog
