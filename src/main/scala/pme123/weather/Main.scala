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
      Bar(
        _.design           := BarDesign.Header,
        _.slots.endContent := Link(
          _.href   := "https://github.com/pme123/pme123-weather",
          _.target := LinkTarget._blank,
          "Github"
        ),
        Title(_.size := TitleLevel.H4, "pme123 Weather Experiments")
      ),
      div(
        overflowY := "auto",
        WeatherTabs(selectedTabVar),
        WeatherView(selectedTabVar)
      ),
      div(
        className := "footer",
        Link(
          _.href   := openMeteoForcastUrl,
          _.target := LinkTarget._blank,
          "Using Data from OpenMeteo (REST APIs)"
        )
      )
    )
end Main
