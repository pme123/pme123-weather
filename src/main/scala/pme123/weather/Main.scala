
package pme123.weather

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom

import scala.scalajs.js.annotation.JSExportTopLevel
import pme123.weather.openmeteo.openMeteoApiUI

object Main:

  @JSExportTopLevel("main")
  def main(args: Array[String] = Array.empty): Unit =
    lazy val appContainer = dom.document.querySelector("#app")
    renderOnDomContentLoaded(appContainer, page)
  end main

  private lazy val selectedTabVar: Var[String] = Var("Urnersee")

  private lazy val page =
    div(
      className := "app-container",
      div(
        className := "header",
        a(
          href      := "https://z9nai.ch",
          target    := "_blank",
          rel       := "noopener",
          className := "z9-brand-link",
          div(
            className := "z9-logo-wrap",
            img(
              src       := "https://z9nai.ch/assets/logo_new-DwOlNuuy.png",
              alt       := "z9nai",
              className := "z9-logo-color"
            ),
            img(
              src       := "https://z9nai.ch/assets/logo_new_white-BXK2S0Ym.png",
              alt       := "z9nai",
              className := "z9-logo-white"
            )
          ),
          h1("Weather Analysis"),
          div(className := "z9-byline", "by z9nai GmbH")
        ),
        div(
          className := "header-right",
          a(
            href      := "https://pme123.github.io/pme123-windspotter",
            target    := "_blank",
            rel       := "noopener",
            className := "header-link",
            "Windspotter"
          ),
          span(className := "header-sep", " | "),
          a(
            href      := "https://pme123.github.io/pme123-windalert/",
            target    := "_blank",
            rel       := "noopener",
            className := "header-link",
            "Wind Alert"
          ),
          span(className := "header-sep", " | "),
          a(
            href      := "https://github.com/pme123/pme123-weather",
            target    := "_blank",
            rel       := "noopener",
            className := "header-link",
            title     := "GitHub",
            svg.svg(
              svg.viewBox := "0 0 24 24",
              svg.width   := "20",
              svg.height  := "20",
              svg.fill    := "currentColor",
              svg.path(
                svg.d := "M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0 0 24 12c0-6.63-5.37-12-12-12z"
              )
            )
          )
        )
      ),
      div(
        className := "main-content",
        WeatherTabs(selectedTabVar),
        WeatherView(selectedTabVar)
      ),
      footerTag(
        className := "z9-footer",
        div(
          className := "z9-footer-inner",
          div(className := "z9-copyright", "© 2026 z9nai GmbH // Alle Rechte vorbehalten"),
          div(
            className := "footer-right",
            a(
              href      := openMeteoApiUI,
              target    := "_blank",
              rel       := "noopener",
              className := "footer-link",
              "Data from Open-Meteo"
            ),
            span(className := "footer-sep-mono", "//"),
            DatenschutzDialog(),
            span(className := "footer-sep-mono", "//"),
            a(
              href      := "mailto:hallo@z9nai.ch",
              className := "z9-mail-icon",
              title     := "hallo@z9nai.ch",
              svg.svg(
                svg.viewBox    := "0 0 24 24",
                svg.fill       := "none",
                svg.stroke     := "currentColor",
                svg.strokeWidth    := "2",
                svg.strokeLineCap  := "round",
                svg.strokeLineJoin := "round",
                svg.width  := "16",
                svg.height := "16",
                svg.rect(
                  svg.width  := "20",
                  svg.height := "16",
                  svg.x  := "2",
                  svg.y  := "4",
                  svg.rx := "2"
                ),
                svg.path(
                  svg.d := "m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7"
                )
              )
            )
          )
        )
      )
    )
end Main
