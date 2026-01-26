package pme123.weather.info

import be.doeraene.webcomponents.ui5.*
import be.doeraene.webcomponents.ui5.Avatar.colorScheme
import be.doeraene.webcomponents.ui5.Label.showColon
import be.doeraene.webcomponents.ui5.MessageStrip.{design, hideCloseButton}
import be.doeraene.webcomponents.ui5.Panel.{accessibleRole, collapsed, headerText}
import be.doeraene.webcomponents.ui5.Title.level
import be.doeraene.webcomponents.ui5.configkeys.{
  AvatarColorScheme,
  MessageStripDesign,
  PanelAccessibleRole,
  TitleLevel
}
import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.HTMLDivElement

def urnersee: ReactiveHtmlElement[HTMLDivElement] = div(
  marginBottom := "20px",

  // Header-Bereich
  Title(
    level         := TitleLevel.H1,
    marginBottom  := "8px",
    div("Ein paar Theorien (KI und eigene Erfahrung)")
  ),
  hr(marginBottom := "20px"),

  // Gruppierungs-Panel: Föhn/Föhnbise
  Panel(
    accessibleRole := PanelAccessibleRole.Region,
    headerText     := "Föhn & Föhnbise - Die Druckgetriebenen Winde",
    collapsed      := true,
    marginBottom   := "16px",
    div(
      padding := "16px",

      // 1. Phänomenologie Panel
      Panel(
        accessibleRole := PanelAccessibleRole.Region,
        headerText     := "1. Das Prinzip der Föhnbise",
        collapsed      := true,
        marginBottom   := "16px",
        div(
          padding := "16px",
          p(
            marginBottom := "12px",
            "Die Föhnbise ist ein lokales Nordwind-Regime, das paradoxerweise bei starkem Luftdrucküberschuss im Süden auftritt. Sie entsteht durch einen atmosphärischen Rotor:"
          ),
          ul(
            marginTop    := "8px",
            li(
              b("Der Kaltluftsee:"),
              " Kalte, dichte Luft lagert wie ein 'Pfropfen' auf dem Urnersee."
            ),
            li(
              b("Die Walzenbildung:"),
              " Der Föhn gleitet oben drüber. Durch Reibung wird die Kaltluft am Boden wie ein Zahnrad entgegengesetzt gedreht – Nordwind entsteht."
            ),
            li(b("Vakuumeffekt:"), " Ein lokales Tief bei Altdorf saugt aktiv Luft von Norden an.")
          )
        )
      ),

      // 1a. Instrumentelle Indikatoren
      Panel(
        headerText     := "2. Der Druckgradient - wann gibts was",
        collapsed      := true,
        marginBottom   := "16px",
        div(
          padding := "16px",
          TableCompat(
            TableColumnCompat(
              width := "30%",
              Label("Druckdifferenz (ΔP) (Lugano - Zürich)")
            ),
            TableColumnCompat(
              width := "25%",
              Label("Windregime am Urnersee")
            ),
            TableColumnCompat(
              width := "45%",
              Label("Durchbruchswahrscheinlichkeit")
            ),
            TableRowCompat(
              TableCellCompat("< +2 hPa"),
              TableCellCompat("Variabel / Thermisch"),
              TableCellCompat("Sehr gering")
            ),
            TableRowCompat(
              TableCellCompat("+2 bis +4 hPa"),
              TableCellCompat("Föhnbise (N)"),
              TableCellCompat("Mässig (Föhn in der Höhe)")
            ),
            TableRowCompat(
              TableCellCompat("+4 bis +8 hPa"),
              TableCellCompat("Starke Föhnbise → Südföhn"),
              TableCellCompat("Hoch (Kritische Zone)")
            ),
            TableRowCompat(
              TableCellCompat("> +8 hPa"),
              TableCellCompat("Heftiger Südföhn (S)"),
              TableCellCompat("Sicher (oft ohne Föhnbise-Phase)")
            )
          )
        )
      ),

      // 2. Diagnose-Tabelle (Das Herzstück)
      Panel(
        headerText     := "3. Die Föhnbise und der Durchbruch des Südföhn",
        collapsed      := true,
        marginBottom   := "16px",
        div(
          padding := "16px",
          p(
            marginBottom := "16px",
            "Der Vergleich zwischen ",
            b("Altdorf und Zürich (ΔP A-Z)"),
            " ist der Schlüssel:"
          ),
          TableCompat(
            TableColumnCompat(
              width := "35%",
              Label("Messwerte (ΔP)")
            ),
            TableColumnCompat(
              width := "25%",
              Label("Zustand")
            ),
            TableColumnCompat(
              width := "40%",
              Label("Phänomenologie")
            ),
            TableRowCompat(
              TableCellCompat("Lug-Zrh > 6 / Alt-Zrh negativ"),
              TableCellCompat("Starke Föhnbise"),
              TableCellCompat("Der Föhn tobt oben. Das Altdorf-Vakuum saugt stabil Nordluft an.")
            ),
            TableRowCompat(
              TableCellCompat("Alt-Zrh nähert sich 0"),
              TableCellCompat("Labilisierung"),
              TableCellCompat(
                "Die Bise wird unruhig. Die Kaltluft wird dünner (Erosion). Durchbruch naht."
              )
            ),
            TableRowCompat(
              TableCellCompat("Alt-Zrh wird positiv"),
              TableCellCompat("Föhndurchbruch"),
              TableCellCompat("Vakuum besiegt. Föhn flutet das Tal. Sofortiger Wechsel auf Süd.")
            )
          )
        )
      ),

      // 3. Grafik & Trend Info
      Panel(
        headerText     := "4. Grafik & Trend-Analyse",
        collapsed      := true,
        marginBottom   := "16px",
        div(
          padding := "16px",
          p(
            marginBottom    := "12px",
            "Achte auf den ",
            b("Crossover-Punkt"),
            ":"
          ),
          MessageStrip(
            design          := MessageStripDesign.Information,
            hideCloseButton := true,
            marginBottom    := "12px",
            "Der Trend ist wichtiger als der Momentanwert! Steigt die Altdorf-Kurve stetig Richtung 0 hPa, steht der Wechsel bevor."
          ),
          ul(
            marginTop       := "8px",
            li(b("Negativer Bereich:"), " Vakuumeffekt aktiv → Nordwind."),
            li(b("Null-Linie:"), " Der kritische Punkt des Windwechsels."),
            li(b("Positiver Bereich:"), " Südföhn hat die Oberhand.")
          )
        )
      ),

      // 4. Warnsignale
      Panel(
        headerText     := "5. Visuelle Signale vor Ort",
        collapsed      := true,
        marginBottom   := "16px",
        div(
          padding := "16px",
          ul(
            li(
              b("Die Grüne Linie:"),
              " Schaumkronen-Front, die von Süden her über den See wandert."
            ),
            li(
              b("Die Föhnnase:"),
              " Sprunghafter Temperaturanstieg (+5°C bis +10°C) bei Durchbruch."
            ),
            li(
              b("Föhnfenster:"),
              " Blauer Himmel über dem See trotz 'Föhnmauer' (Wolkenwand) im Süden."
            )
          )
        )
      )
    ),
    // Footer / Praxis-Tipp
    MessageStrip(
      design          := MessageStripDesign.Warning,
      hideCloseButton := true,
      marginTop       := "20px",
      "Praxis-Tipp: Je stärker die Föhnbise (tieferer Altdorf-Druck), desto explosiver ist meist der spätere Durchbruch. Die Bise wirkt wie eine gespannte Feder!"
    )
  ),

  // Gruppierungs-Panel: Thermik
  Panel(
    accessibleRole := PanelAccessibleRole.Region,
    headerText     := "Thermik - Die Temperaturgetriebenen Winde",
    collapsed      := true,
    marginBottom   := "16px",
    div(
      padding := "16px",

      // 6. Der aktuelle "Sog" (Horizontaler Gradient)
      Panel(
        headerText   := "6. Der aktuelle \"Sog\" (Horizontaler Gradient)",
        collapsed    := true,
        marginBottom := "16px",
        div(
          padding := "16px",
          p(
            marginBottom    := "12px",
            b("Messwert: "),
            "T",
            sub("Altdorf"),
            " - T",
            sub("Luzern"),
            " (oder Zürich) zu einem festen Zeitpunkt"
          ),
          p(
            marginBottom    := "12px",
            b("Bedeutung: "),
            "Das ist der ",
            b("Ist-Zustand"),
            ". Er sagt dir, ob jetzt gerade ein Temperaturunterschied besteht, der Luft ansaugt."
          ),
          MessageStrip(
            design          := MessageStripDesign.Information,
            hideCloseButton := true,
            marginBottom    := "12px",
            "Die 5–6°C Regel: Wenn Altdorf im Moment (z. B. um 13:00 Uhr) 6 Grad wärmer ist als Luzern, dann herrscht dort ein lokales thermisches Tiefdruckgebiet. Der Wind muss fließen, um den Unterschied auszugleichen."
          ),
          p(
            marginBottom    := "8px",
            b("Anwendung: "),
            "Ein Blick auf die Live-Daten. ",
            i("\"Ist der Motor an?\"")
          ),
          ul(
            marginTop       := "8px",
            li(b("ΔT > 5-6°C:"), " Starker thermischer Sog → Wind wird kommen oder ist bereits da"),
            li(b("ΔT 3-5°C:"), " Moderater Gradient → Thermik entwickelt sich"),
            li(b("ΔT < 3°C:"), " Schwacher Gradient → Wenig thermische Aktivität")
          )
        )
      ),

      // 7. Die "System-Energie" (Tagesamplitude)
      Panel(
        headerText   := "7. Die \"System-Energie\" (Tagesamplitude)",
        collapsed    := true,
        marginBottom := "16px",
        div(
          padding := "16px",
          p(
            marginBottom    := "12px",
            b("Messwert: "),
            "T",
            sub("max, Altdorf"),
            " - T",
            sub("min, Altdorf"),
            " (Differenz innerhalb von 24h)"
          ),
          p(
            marginBottom    := "12px",
            b("Bedeutung: "),
            "Das ist der ",
            b("Potenzial-Check"),
            ". Er sagt dir, wie \"sauber\" und \"energiereich\" das Wettermodell des Tages ist."
          ),
          MessageStrip(
            design          := MessageStripDesign.Positive,
            hideCloseButton := true,
            marginBottom    := "12px",
            "Warum die Nacht wichtig ist: Wenn es nachts in Altdorf richtig abkühlt (z. B. auf 12°C) und tagsüber heiß wird (28°C), bedeutet das: Die Luft ist trocken (gut für Thermik), das Tal wurde nachts mit frischer Kaltluft \"geflutet\" (der Reset), und die \"Feder\" wird über 16 Grad gespannt."
          ),
          p(
            marginBottom    := "8px",
            b("Anwendung: "),
            "Der Blick am Morgen. ",
            i("\"Wie stark wird es heute Nachmittag werden?\"")
          ),
          TableCompat(
            TableColumnCompat(
              width := "30%",
              Label("Tagesamplitude")
            ),
            TableColumnCompat(
              width := "35%",
              Label("Luftqualität")
            ),
            TableColumnCompat(
              width := "35%",
              Label("Thermik-Prognose")
            ),
            TableRowCompat(
              TableCellCompat("< 10°C"),
              TableCellCompat("Feucht/schwül"),
              TableCellCompat("Schwach, böig, instabil")
            ),
            TableRowCompat(
              TableCellCompat("10-15°C"),
              TableCellCompat("Mässig trocken"),
              TableCellCompat("Gut, aber nicht optimal")
            ),
            TableRowCompat(
              TableCellCompat("> 16°C"),
              TableCellCompat("Trocken, klar"),
              TableCellCompat("Hammer-Tag! Stark & konstant")
            )
          )
        )
      ),

      // 8. Die Kombination: Motor und Gaspedal
      Panel(
        headerText   := "8. Die Kombination: Motor und Gaspedal",
        collapsed    := true,
        marginBottom := "16px",
        div(
          padding := "16px",
          p(
            marginBottom := "12px",
            "Man kann es mit einem Auto vergleichen:"
          ),
          ul(
            marginTop    := "8px",
            marginBottom := "16px",
            li(
              b("Die Amplitude (Tag/Nacht)"),
              " sagt dir, wie viel PS der Motor hat und wie voll der Tank ist."
            ),
            li(
              b("Der Gradient (Altdorf/Luzern)"),
              " sagt dir, wie stark du gerade aufs Gaspedal drückst."
            )
          ),
          p(
            marginBottom := "12px",
            b("Beispiele aus der Praxis am Urnersee:")
          ),
          div(
            p("Szenario A: "),
            "Altdorf ist 6°C wärmer als Luzern, aber die Nacht war extrem schwül und warm (Amplitude nur 7°C).",
            br(),
            b("→ Ergebnis: "),
            "Der Wind wird wahrscheinlich kommen, aber er ist oft \"eckig\", böig oder bricht früh wieder zusammen."
          ),
          div(
            p("Szenario B: "),
            "Altdorf ist 6°C wärmer als Luzern und die Nacht war kristallklar und kalt (Amplitude 18°C).",
            br(),
            b("→ Ergebnis: "),
            "Das ist der klassische \"Hammer-Tag\". Der Wind ist konstant, stark und reicht bis weit in den Abend hinein."
          )
        )
      ),

      // 9. Der vertikale Gatekeeper (Altdorf vs. Gütsch)
      Panel(
        headerText   := "9. Der vertikale Gatekeeper (Altdorf vs. Gütsch)",
        collapsed    := true,
        marginBottom := "16px",
        div(
          padding := "16px",
          p(
            marginBottom := "12px",
            "Dies ist dein ",
            b("\"Radar\" für die Luftschichtung"),
            ". Hier geht es darum, ob der Weg für den Wind frei ist."
          ),

          // Die Inversion (Der Deckel)
          div(
            marginBottom := "16px",
            p(
              marginBottom    := "8px",
              b("Die Inversion (Der Deckel):")
            ),
            p(
              marginBottom    := "8px",
              "Wenn es auf dem Gütsch (2287m) wärmer oder fast so warm ist wie in Altdorf, haben wir eine ",
              b("Inversion"),
              ". Kalte Luft am Boden ist schwerer als warme Luft darüber."
            ),
            MessageStrip(
              design          := MessageStripDesign.Negative,
              hideCloseButton := true,
              div(
                b("→ Folge: "),
                "Die Luftmassen können sich nicht vermischen. Der Föhn bleibt oben \"kleben\" und am See herrscht trotz Sturmwarnung nur Föhnbise oder Flaute."
              )
            )
          ),

          // Die Labilität (Die offene Tür)
          div(
            marginBottom := "16px",
            p(
              marginBottom    := "8px",
              b("Die Labilität (Die offene Tür):")
            ),
            p(
              marginBottom    := "8px",
              "Wenn die Temperatur mit der Höhe normal abnimmt (Altdorf warm, Gütsch kalt), ist die Atmosphäre ",
              b("\"offen\""),
              "."
            ),
            MessageStrip(
              design          := MessageStripDesign.Positive,
              hideCloseButton := true,
              div(
                b("→ Folge: "),
                "Thermik kann ungehindert aufsteigen, und der Föhn kann leicht bis zum Wasser durchgreifen."
              )
            )
          ),

          // Der Gütsch-Wind
          div(
            marginBottom := "8px",
            p(
              marginBottom := "8px",
              b("Der Gütsch-Wind:")
            ),
            p(
              "Er zeigt dir die ",
              b("\"echte\" Föhnstärke in der Höhe"),
              ", noch bevor sie im Tal ankommt."
            )
          ),

          // Praktische Tabelle
          TableCompat(
            marginTop    := "16px",
            TableColumnCompat(
              width := "35%",
              Label("Temperaturverhältnis")
            ),
            TableColumnCompat(
              width := "30%",
              Label("Luftschichtung")
            ),
            TableColumnCompat(
              width := "35%",
              Label("Wind-Prognose")
            ),
            TableRowCompat(
              TableCellCompat("T", sub("Gütsch"), " ≥ T", sub("Altdorf")),
              TableCellCompat("Inversion (Deckel)"),
              TableCellCompat("Föhn bleibt oben, Flaute/Föhnbise")
            ),
            TableRowCompat(
              TableCellCompat("T", sub("Gütsch"), " ≈ T", sub("Altdorf"), " - 5°C"),
              TableCellCompat("Neutral"),
              TableCellCompat("Mässige Durchmischung")
            ),
            TableRowCompat(
              TableCellCompat("T", sub("Gütsch"), " < T", sub("Altdorf"), " - 10°C"),
              TableCellCompat("Labil (offen)"),
              TableCellCompat("Föhn greift durch, starke Thermik")
            )
          )
        )
      )
    )
  )
)
