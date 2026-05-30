
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
            "GitHub"
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
              "Open-Meteo"
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
