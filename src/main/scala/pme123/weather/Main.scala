package pme123.weather

// src/main/scala/Main.scala

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom

import scala.scalajs.js.annotation.JSExportTopLevel


object Main :

  @JSExportTopLevel("main")
  def main(args: Array[String] = Array.empty): Unit =
    lazy val appContainer = dom.document.querySelector("#laminar")
    renderOnDomContentLoaded(appContainer, WeatherView())
