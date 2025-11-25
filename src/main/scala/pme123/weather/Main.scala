
package pme123.weather

// src/main/scala/Main.scala

import be.doeraene.webcomponents.ui5.*
import be.doeraene.webcomponents.ui5.configkeys.*
import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom

import scala.scalajs.js.annotation.JSExportTopLevel
import pme123.weather.openmeteo.openMeteoApiUI
import pme123.weather.meteoschweiz.meteoSwissApiUI

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
        className := "main-content",/*
        div(
          className := "test-controls",
          Button(
            _.design := ButtonDesign.Emphasized,
            _.events.onClick --> { _ =>
              println("🧪 Running MeteoSwiss Client Tests...")
              pme123.weather.meteoschweiz.MeteoSwissClientTest.runAllTests()
            },
            "🇨🇭 Test MeteoSwiss Client (Mock)"
          ),
          " ",
          Button(
            _.design := ButtonDesign.Default,
            _.events.onClick --> { _ =>
              println("🌐 Testing Real MeteoSwiss API...")
              pme123.weather.meteoschweiz.MeteoSwissClientTest.testRealAPI()
            },
            "🌐 Test Real API"
          )
        ), */
        WeatherTabs(selectedTabVar),
        WeatherView(selectedTabVar)
      ),
      div(
        className := "footer",
        "Data provided by ",
        Link(
          _.href   := openMeteoApiUI,
          _.target := LinkTarget._blank,
          "OpenMeteo"
        ), /*
        " | 🇨🇭 ",
        Link(
          _.href   := meteoSwissApiUI,
          _.target := LinkTarget._blank,
          "MeteoSwiss ICON-CH2"
        ),
        " (In Development)"*/
      )
    )
end Main
