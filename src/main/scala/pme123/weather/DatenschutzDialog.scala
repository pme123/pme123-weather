package pme123.weather

import com.raquo.laminar.api.L.{*, given}

object DatenschutzDialog:

  def apply(): HtmlElement =
    val openVar = Var(false)

    span(
      button(
        className := "footer-btn",
        "Datenschutz",
        onClick --> { _ => openVar.set(true) }
      ),
      child <-- openVar.signal.map:
        case false => emptyNode
        case true =>
          div(
            className := "info-modal-overlay",
            onClick --> { _ => openVar.set(false) },
            div(
              className := "info-modal datenschutz-modal",
              onClick --> { ev => ev.stopPropagation() },

              div(
                className := "info-modal-header",
                span(className := "info-modal-title", "Datenschutzerklärung"),
                button(
                  className := "info-modal-close",
                  "✕",
                  onClick --> { _ => openVar.set(false) }
                )
              ),

              div(
                className := "info-modal-body",

                p(className := "ds-company", "z9nai GmbH · Schweiz · ",
                  a(href := "mailto:hallo@z9nai.ch", className := "ds-link", "hallo@z9nai.ch")),

                h3(className := "ds-heading", "Keine Cookies, kein Tracking"),
                p(className := "ds-p",
                  "Diese Website verwendet keine Cookies und keine Tracking- oder Analyse-Tools. ",
                  "Es werden keine persönlichen Daten durch Drittdienste erhoben oder weitergegeben."),

                h3(className := "ds-heading", "Hosting – GitHub Pages"),
                p(className := "ds-p",
                  "Diese Website wird über GitHub Pages gehostet (GitHub Inc., San Francisco, USA). ",
                  "Beim Aufruf erhebt GitHub automatisch technische Zugriffsdaten (IP-Adresse, Zeitstempel). ",
                  "Weitere Informationen: ",
                  a(href := "https://docs.github.com/en/site-policy/privacy-policies/github-general-privacy-statement",
                    target := "_blank", rel := "noopener", className := "ds-link",
                    "GitHub Privacy Policy"), "."),

                h3(className := "ds-heading", "Wetterdaten – Open-Meteo"),
                p(className := "ds-p",
                  "Wetterdaten werden von ",
                  a(href := "https://open-meteo.com", target := "_blank", rel := "noopener",
                    className := "ds-link", "Open-Meteo"),
                  " bezogen. Beim Datenabruf wird deine IP-Adresse an Open-Meteo übermittelt."),

                h3(className := "ds-heading", "Kontakt per E-Mail"),
                p(className := "ds-p",
                  "Anfragen per E-Mail werden ausschliesslich zur Bearbeitung deiner Anfrage verwendet ",
                  "und nicht an Dritte weitergegeben."),

                h3(className := "ds-heading", "Deine Rechte (nDSG)"),
                p(className := "ds-p",
                  "Nach dem Schweizer Datenschutzgesetz hast du das Recht auf Auskunft, Berichtigung und Löschung. ",
                  "Kontakt: ",
                  a(href := "mailto:hallo@z9nai.ch", className := "ds-link", "hallo@z9nai.ch"), "."),

                div(
                  className := "ds-imprint",
                  "z9nai GmbH · Sonnenweg 23a · Schweiz · UID: CHE-453.251.270",
                  br(),
                  "Stand: Mai 2026"
                )
              )
            )
          )
    )
  end apply

end DatenschutzDialog