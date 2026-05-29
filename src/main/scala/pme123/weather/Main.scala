
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
          span(
            className := "footer-data",
            "Data: ",
            a(
              href      := openMeteoApiUI,
              target    := "_blank",
              rel       := "noopener",
              className := "footer-link",
              "OpenMeteo"
            )
          ),
          a(
            href      := "mailto:info@z9nai.ch",
            className := "z9-mail-icon",
            title     := "info@z9nai.ch",
            "✉"
          )
        )
      )
    )
end Main
