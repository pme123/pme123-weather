package pme123.weather.info

import be.doeraene.webcomponents.ui5.*
import be.doeraene.webcomponents.ui5.Avatar.colorScheme
import be.doeraene.webcomponents.ui5.Label.showColon
import be.doeraene.webcomponents.ui5.MessageStrip.{design, hideCloseButton}
import be.doeraene.webcomponents.ui5.Panel.{accessibleRole, collapsed, headerText}
import be.doeraene.webcomponents.ui5.Title.level
import be.doeraene.webcomponents.ui5.configkeys.{AvatarColorScheme, MessageStripDesign, PanelAccessibleRole, TitleLevel}
import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.HTMLDivElement

def urnersee: ReactiveHtmlElement[HTMLDivElement] = div(
  marginBottom := "20px",

  // Header-Bereich
  Title(
    level := TitleLevel.H1,
    marginBottom := "8px",
    div("Ein paar Theorien (KI und eigene Erfahrung)")
  ),
  Label(
    showColon := false,
    marginBottom := "20px",
    "Die Winde am Urnersee verstehen"
  ),

  hr(marginBottom := "20px"),

  // 1. Phänomenologie Panel
  Panel(
    accessibleRole := PanelAccessibleRole.Region,
    headerText := "1. Das Prinzip der Föhnbise",
    collapsed := true,
    marginBottom := "16px",
    div(
      padding := "16px",
      p(
        marginBottom := "12px",
        "Die Föhnbise ist ein lokales Nordwind-Regime, das paradoxerweise bei starkem Luftdrucküberschuss im Süden auftritt. Sie entsteht durch einen atmosphärischen Rotor:"
      ),
      ul(
        marginTop := "8px",
        li(b("Der Kaltluftsee:"), " Kalte, dichte Luft lagert wie ein 'Pfropfen' auf dem Urnersee."),
        li(b("Die Walzenbildung:"), " Der Föhn gleitet oben drüber. Durch Reibung wird die Kaltluft am Boden wie ein Zahnrad entgegengesetzt gedreht – Nordwind entsteht."),
        li(b("Vakuumeffekt:"), " Ein lokales Tief bei Altdorf saugt aktiv Luft von Norden an.")
      )
    )
  ),

  // 2. Diagnose-Tabelle (Das Herzstück)
  Panel(
    headerText := "2. Den Durchbruch an den Druckwerten lesen",
    collapsed := true,
    marginBottom := "16px",
    div(
      padding := "16px",
      p(
        marginBottom := "16px",
        "Der Vergleich zwischen ", b("Altdorf und Zürich (ΔP A-Z)"), " ist der Schlüssel:"
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
          TableCellCompat("Die Bise wird unruhig. Die Kaltluft wird dünner (Erosion). Durchbruch naht.")
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
    headerText := "3. Grafik & Trend-Analyse",
    collapsed := true,
    marginBottom := "16px",
    div(
      padding := "16px",
      p(
        marginBottom := "12px",
        "Achte auf den ", b("Crossover-Punkt"), ":"
      ),
      MessageStrip(
        design := MessageStripDesign.Information,
        hideCloseButton := true,
        marginBottom := "12px",
        "Der Trend ist wichtiger als der Momentanwert! Steigt die Altdorf-Kurve stetig Richtung 0 hPa, steht der Wechsel bevor."
      ),
      ul(
        marginTop := "8px",
        li(b("Negativer Bereich:"), " Vakuumeffekt aktiv → Nordwind."),
        li(b("Null-Linie:"), " Der kritische Punkt des Windwechsels."),
        li(b("Positiver Bereich:"), " Südföhn hat die Oberhand.")
      )
    )
  ),

  // 4. Warnsignale
  Panel(
    headerText := "4. Visuelle Signale vor Ort",
    collapsed := true,
    marginBottom := "16px",
    div(
      padding := "16px",
      ul(
        li(b("Die Grüne Linie:"), " Schaumkronen-Front, die von Süden her über den See wandert."),
        li(b("Die Föhnnase:"), " Sprunghafter Temperaturanstieg (+5°C bis +10°C) bei Durchbruch."),
        li(b("Föhnfenster:"), " Blauer Himmel über dem See trotz 'Föhnmauer' (Wolkenwand) im Süden.")
      )
    )
  ),

  // Footer / Praxis-Tipp
  MessageStrip(
    design := MessageStripDesign.Warning,
    hideCloseButton := true,
    marginTop := "20px",
    "Praxis-Tipp: Je stärker die Föhnbise (tieferer Altdorf-Druck), desto explosiver ist meist der spätere Durchbruch. Die Bise wirkt wie eine gespannte Feder!"
  )
)