
package pme123.weather

// src/main/scala/Main.scala

import be.doeraene.webcomponents.ui5.*
import be.doeraene.webcomponents.ui5.configkeys.*
import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom

import scala.scalajs.js.annotation.JSExportTopLevel

object Main:

  @JSExportTopLevel("main")
  def main(args: Array[String] = Array.empty): Unit =
    lazy val appContainer = dom.document.querySelector("#app")
    renderOnDomContentLoaded(appContainer, page)
  end main

  private lazy val selectedTabVar: Var[String] = Var("Urnersee")

  private lazy val page =
    div(
      width  := "100%",
      height := "100%",
      className := "app-container",
      Bar(
        _.design           := BarDesign.Header,
        _.slots.endContent := div(
          Link(
            _.href   := "https://pme123.github.io/pme123-windspotter",
            _.target := LinkTarget._blank,
            "Windspotter"
          ),
          " | ",
          Link(
            _.href   := "https://github.com/pme123/pme123-weather",
            _.target := LinkTarget._blank,
            "GitHub"
          )
        ),
        Title(_.size := TitleLevel.H4, "Weather Analysis Dashboard")
      ),
      div(
        className := "main-content",
        WeatherTabs(selectedTabVar),
        WeatherView(selectedTabVar)
      ),
      div(
        className := "footer",
        Link(
          _.href   := openMeteoApiUI,
          _.target := LinkTarget._blank,
          "Data provided by OpenMeteo (REST APIs)"
        )
      )
    )
end Main
