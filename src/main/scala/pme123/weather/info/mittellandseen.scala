package pme123.weather.info

import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.HTMLDivElement

def mittellandseen: ReactiveHtmlElement[HTMLDivElement] = div(

  // ─── Bise & Westwind ─────────────────────────────────────────────────────
  detailsTag(
    className := "info-group",
    summaryTag(className := "info-group-title", "Bise & Westwind — Die synoptischen Winde"),

    div(
      className := "info-sub",
      div(className := "info-sub-title", "1. Warum die Mittellandseen anders ticken als der Urnersee"),
      p(
        className := "info-p",
        "Der Urnersee liegt in einem Alpental - dort entstehen Föhn und Thermik durch das Gelände selbst. Seen wie der Sempachersee liegen dagegen im offenen Mittelland: Es gibt keinen Talwind-Mechanismus, der den Wind lokal verstärkt. Stattdessen liegt der See einfach im Windfeld der grossräumigen Wetterlage."
      ),
      ul(
        className := "info-ul",
        li(b("Bise:"), " Kalter Nordostwind bei Hochdruck - entsteht durch das Druckgefälle zwischen der Nordschweiz/Bodensee und dem Genferseebecken."),
        li(b("Südwest-/Westwind:"), " Weht mit durchziehenden Tiefdrucksystemen nördlich der Alpen (\"Südwestlage\").")
      )
    ),

    div(
      className := "info-sub",
      div(className := "info-sub-title", "2. Der Druckgradient — Güttingen minus Genève"),
      p(className := "info-p", "Der Klassiker unter den Schweizer Wind-Indikatoren:"),
      table(
        className := "info-table",
        thead(tr(th("Druckdifferenz (ΔP) Güttingen - Genève"), th("Windregime"))),
        tbody(
          tr(td("< -2 hPa"),        td("Südwest-/Westwind")),
          tr(td("-2 bis +2 hPa"),   td("Kein dominantes Regime (Flaute oder lokale Thermik)")),
          tr(td("+2 bis +5 hPa"),   td("Bise")),
          tr(td("> +5 / < -5 hPa"), td("Starke Bise / starker Westwind"))
        )
      ),
      div(
        className := "info-msg info-msg-info",
        "Güttingen liegt am Bodensee (Nordost), Genève am anderen Ende der Schweiz (Südwest) - die Differenz zeigt damit direkt das grossräumige Nordost-Südwest-Gefälle über die ganze Schweiz."
      )
    ),

    div(
      className := "info-sub",
      div(className := "info-sub-title", "3. Mosen als lokaler Messpunkt"),
      p(
        className := "info-p",
        "Mosen liegt direkt am Ufer des Sempachersees. Der dort gemessene Wind dient als Realitätscheck: Stimmt er mit dem erwarteten Regime überein, oder bremst z.B. Geländeabschattung den Wind vor Ort ab?"
      )
    ),

    div(
      className := "info-sub",
      div(
        className := "info-msg info-msg-warn",
        b("Was hier (noch) fehlt: "),
        "An klaren Hochdrucktagen ohne Bise oder Westwind entwickelt der Sempachersee oft eine eigene See-Land-Zirkulation (tagsüber auflandig, nachts ablandig) - vergleichbar mit einer Mini-Seebrise. Das braucht einen Temperaturkontrast Seeufer (Mosen) gegen Hinterland (Egolzwil/Wynau) und ist in dieser ersten Version noch nicht modelliert."
      )
    )
  )
)
