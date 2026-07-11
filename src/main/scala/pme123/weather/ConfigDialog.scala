package pme123.weather

import com.raquo.laminar.api.L.{*, given}

// Header gear icon that opens ConfigEditorDialog - kept as its own small component so
// the trigger (here, or elsewhere) is decoupled from the dialog's own open/close state.
object ConfigDialog:

  def apply(): HtmlElement =
    button(
      className := "header-icon-btn",
      title     := "Konfiguration",
      onClick --> { _ => ConfigEditorDialog.open() },
      gearIcon
    )

  private def gearIcon =
    svg.svg(
      svg.viewBox        := "0 0 24 24",
      svg.width           := "18",
      svg.height          := "18",
      svg.fill            := "none",
      svg.stroke          := "currentColor",
      svg.strokeWidth     := "2",
      svg.strokeLineCap   := "round",
      svg.strokeLineJoin  := "round",
      svg.circle(svg.cx := "12", svg.cy := "12", svg.r := "3"),
      svg.path(
        svg.d := "M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1Z"
      )
    )

end ConfigDialog
