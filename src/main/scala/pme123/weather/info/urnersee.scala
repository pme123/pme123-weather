package pme123.weather.info

import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.HTMLDivElement

def urnersee: ReactiveHtmlElement[HTMLDivElement] = div(

  // ─── Föhn & Föhnbise ───────────────────────────────────────────────────────
  detailsTag(
    className := "info-group",
    summaryTag(className := "info-group-title", "Föhn & Föhnbise — Die Druckgetriebenen Winde"),

    div(
      className := "info-sub",
      div(className := "info-sub-title", "1. Das Prinzip der Föhnbise"),
      p(
        className := "info-p",
        "Die Föhnbise ist ein lokales Nordwind-Regime, das paradoxerweise bei starkem Luftdrucküberschuss im Süden auftritt. Sie entsteht durch einen atmosphärischen Rotor:"
      ),
      ul(
        className := "info-ul",
        li(b("Der Kaltluftsee:"), " Kalte, dichte Luft lagert wie ein 'Pfropfen' auf dem Urnersee."),
        li(b("Die Walzenbildung:"), " Der Föhn gleitet oben drüber. Durch Reibung wird die Kaltluft am Boden wie ein Zahnrad entgegengesetzt gedreht – Nordwind entsteht."),
        li(b("Vakuumeffekt:"), " Ein lokales Tief bei Altdorf saugt aktiv Luft von Norden an.")
      )
    ),

    div(
      className := "info-sub",
      div(className := "info-sub-title", "2. Der Druckgradient — wann gibts was"),
      table(
        className := "info-table",
        thead(tr(th("Druckdifferenz (ΔP) Lugano - Zürich"), th("Windregime am Urnersee"), th("Durchbruchswahrscheinlichkeit"))),
        tbody(
          tr(td("< +2 hPa"),       td("Variabel / Thermisch"),       td("Sehr gering")),
          tr(td("+2 bis +4 hPa"),  td("Föhnbise (N)"),               td("Mässig (Föhn in der Höhe)")),
          tr(td("+4 bis +8 hPa"),  td("Starke Föhnbise → Südföhn"),  td("Hoch (Kritische Zone)")),
          tr(td("> +8 hPa"),       td("Heftiger Südföhn (S)"),       td("Sicher (oft ohne Föhnbise-Phase)"))
        )
      )
    ),

    div(
      className := "info-sub",
      div(className := "info-sub-title", "3. Die Föhnbise und der Durchbruch des Südföhn"),
      p(className := "info-p", "Der Vergleich zwischen ", b("Altdorf und Zürich (ΔP A-Z)"), " ist der Schlüssel:"),
      table(
        className := "info-table",
        thead(tr(th("Messwerte (ΔP)"), th("Zustand"), th("Phänomenologie"))),
        tbody(
          tr(
            td("Lug-Zrh > 6 / Alt-Zrh negativ"),
            td("Starke Föhnbise"),
            td("Der Föhn tobt oben. Das Altdorf-Vakuum saugt stabil Nordluft an.")
          ),
          tr(
            td("Alt-Zrh nähert sich 0"),
            td("Labilisierung"),
            td("Die Bise wird unruhig. Die Kaltluft wird dünner (Erosion). Durchbruch naht.")
          ),
          tr(
            td("Alt-Zrh wird positiv"),
            td("Föhndurchbruch"),
            td("Vakuum besiegt. Föhn flutet das Tal. Sofortiger Wechsel auf Süd.")
          )
        )
      )
    ),

    div(
      className := "info-sub",
      div(className := "info-sub-title", "4. Grafik & Trend-Analyse"),
      p(className := "info-p", "Achte auf den ", b("Crossover-Punkt"), ":"),
      div(
        className := "info-msg info-msg-info",
        "Der Trend ist wichtiger als der Momentanwert! Steigt die Altdorf-Kurve stetig Richtung 0 hPa, steht der Wechsel bevor."
      ),
      ul(
        className := "info-ul",
        li(b("Negativer Bereich:"), " Vakuumeffekt aktiv → Nordwind."),
        li(b("Null-Linie:"), " Der kritische Punkt des Windwechsels."),
        li(b("Positiver Bereich:"), " Südföhn hat die Oberhand.")
      )
    ),

    div(
      className := "info-sub",
      div(className := "info-sub-title", "5. Visuelle Signale vor Ort"),
      ul(
        className := "info-ul",
        li(b("Die Grüne Linie:"), " Schaumkronen-Front, die von Süden her über den See wandert."),
        li(b("Die Föhnnase:"), " Sprunghafter Temperaturanstieg (+5°C bis +10°C) bei Durchbruch."),
        li(b("Föhnfenster:"), " Blauer Himmel über dem See trotz 'Föhnmauer' (Wolkenwand) im Süden.")
      )
    ),

    div(
      className := "info-sub",
      div(
        className := "info-msg info-msg-warn",
        b("Praxis-Tipp: "),
        "Je stärker die Föhnbise (tieferer Altdorf-Druck), desto explosiver ist meist der spätere Durchbruch. Die Bise wirkt wie eine gespannte Feder!"
      )
    )
  ),

  // ─── Thermik ───────────────────────────────────────────────────────────────
  detailsTag(
    className := "info-group",
    summaryTag(className := "info-group-title", "Thermik — Die Temperaturgetriebenen Winde"),

    div(
      className := "info-sub",
      div(className := "info-sub-title", "6. Der aktuelle \"Sog\" (Horizontaler Gradient)"),
      p(className := "info-p", b("Messwert: "), "T", sub("Altdorf"), " - T", sub("Luzern"), " (oder Zürich) zu einem festen Zeitpunkt"),
      p(className := "info-p", b("Bedeutung: "), "Das ist der ", b("Ist-Zustand"), ". Er sagt dir, ob jetzt gerade ein Temperaturunterschied besteht, der Luft ansaugt."),
      div(
        className := "info-msg info-msg-info",
        "Die 5–6°C Regel: Wenn Altdorf im Moment (z. B. um 13:00 Uhr) 6 Grad wärmer ist als Luzern, dann herrscht dort ein lokales thermisches Tiefdruckgebiet. Der Wind muss fließen, um den Unterschied auszugleichen."
      ),
      p(className := "info-p", b("Anwendung: "), "Ein Blick auf die Live-Daten. ", i("\"Ist der Motor an?\"")),
      ul(
        className := "info-ul",
        li(b("ΔT > 5-6°C:"), " Starker thermischer Sog → Wind wird kommen oder ist bereits da"),
        li(b("ΔT 3-5°C:"), " Moderater Gradient → Thermik entwickelt sich"),
        li(b("ΔT < 3°C:"), " Schwacher Gradient → Wenig thermische Aktivität")
      )
    ),

    div(
      className := "info-sub",
      div(className := "info-sub-title", "7. Die \"System-Energie\" (Tagesamplitude)"),
      p(className := "info-p", b("Messwert: "), "T", sub("max, Altdorf"), " - T", sub("min, Altdorf"), " (Differenz innerhalb von 24h)"),
      p(className := "info-p", b("Bedeutung: "), "Das ist der ", b("Potenzial-Check"), ". Er sagt dir, wie \"sauber\" und \"energiereich\" das Wettermodell des Tages ist."),
      div(
        className := "info-msg info-msg-ok",
        b("Warum die Nacht wichtig ist: "),
        "Wenn es nachts in Altdorf richtig abkühlt (z. B. auf 12°C) und tagsüber heiß wird (28°C): Die Luft ist trocken (gut für Thermik), das Tal wurde nachts mit frischer Kaltluft \"geflutet\" (der Reset), und die \"Feder\" wird über 16 Grad gespannt."
      ),
      p(className := "info-p", b("Anwendung: "), "Der Blick am Morgen. ", i("\"Wie stark wird es heute Nachmittag werden?\"")),
      table(
        className := "info-table",
        thead(tr(th("Tagesamplitude"), th("Luftqualität"), th("Thermik-Prognose"))),
        tbody(
          tr(td("< 10°C"),  td("Feucht/schwül"),   td("Schwach, böig, instabil")),
          tr(td("10-15°C"), td("Mässig trocken"),   td("Gut, aber nicht optimal")),
          tr(td("> 16°C"),  td("Trocken, klar"),    td("Hammer-Tag! Stark & konstant"))
        )
      )
    ),

    div(
      className := "info-sub",
      div(className := "info-sub-title", "8. Die Kombination: Motor und Gaspedal"),
      p(className := "info-p", "Man kann es mit einem Auto vergleichen:"),
      ul(
        className := "info-ul",
        li(b("Die Amplitude (Tag/Nacht)"), " sagt dir, wie viel PS der Motor hat und wie voll der Tank ist."),
        li(b("Der Gradient (Altdorf/Luzern)"), " sagt dir, wie stark du gerade aufs Gaspedal drückst.")
      ),
      p(className := "info-p",
        b("Szenario A: "),
        "Altdorf 6°C wärmer als Luzern, Nacht schwül (Amplitude 7°C).",
        br(),
        b("→ "),
        "Wind kommt wahrscheinlich, aber oft \"eckig\", böig oder bricht früh zusammen."
      ),
      p(className := "info-p",
        b("Szenario B: "),
        "Altdorf 6°C wärmer, Nacht kristallklar und kalt (Amplitude 18°C).",
        br(),
        b("→ "),
        "Klassischer \"Hammer-Tag\": konstanter, starker Wind bis weit in den Abend."
      )
    ),

    div(
      className := "info-sub",
      div(className := "info-sub-title", "9. Der vertikale Gatekeeper (Altdorf vs. Gütsch)"),
      p(className := "info-p", "Dies ist dein ", b("\"Radar\" für die Luftschichtung"), ". Hier geht es darum, ob der Weg für den Wind frei ist."),
      p(className := "info-p",
        b("Die Inversion (Der Deckel): "),
        "Wenn es auf dem Gütsch (2287m) wärmer oder fast so warm ist wie in Altdorf, haben wir eine ",
        b("Inversion"),
        ". Kalte Luft am Boden ist schwerer als warme Luft darüber."
      ),
      div(className := "info-msg info-msg-err",
        b("→ Folge: "),
        "Die Luftmassen können sich nicht vermischen. Der Föhn bleibt oben \"kleben\" und am See herrscht trotz Sturmwarnung nur Föhnbise oder Flaute."
      ),
      p(className := "info-p",
        b("Die Labilität (Die offene Tür): "),
        "Wenn die Temperatur mit der Höhe normal abnimmt (Altdorf warm, Gütsch kalt), ist die Atmosphäre ",
        b("\"offen\""),
        "."
      ),
      div(className := "info-msg info-msg-ok",
        b("→ Folge: "),
        "Thermik kann ungehindert aufsteigen, und der Föhn kann leicht bis zum Wasser durchgreifen."
      ),
      p(className := "info-p",
        b("Der Gütsch-Wind: "),
        "Er zeigt dir die ", b("\"echte\" Föhnstärke in der Höhe"), ", noch bevor sie im Tal ankommt."
      ),
      table(
        className := "info-table",
        thead(tr(th("Temperaturverhältnis"), th("Luftschichtung"), th("Wind-Prognose"))),
        tbody(
          tr(
            td("T", sub("Gütsch"), " ≥ T", sub("Altdorf")),
            td("Inversion (Deckel)"),
            td("Föhn bleibt oben, Flaute/Föhnbise")
          ),
          tr(
            td("T", sub("Gütsch"), " ≈ T", sub("Altdorf"), " - 5°C"),
            td("Neutral"),
            td("Mässige Durchmischung")
          ),
          tr(
            td("T", sub("Gütsch"), " < T", sub("Altdorf"), " - 10°C"),
            td("Labil (offen)"),
            td("Föhn greift durch, starke Thermik")
          )
        )
      )
    )
  )
)
