#!/usr/bin/env -S scala shebang

//> using dep "com.lihaoyi::os-lib:0.11.3"


@main
def main(args: String*) =
  println("Running fullOptJS")
  os.proc("sbt", "fastOptJS").call()
  println("Copy result to root")
  os.copy.over(os.pwd / "target" / "scala-3.6.2" / "pme123-weather-fastopt.js", os.pwd / "pme123-weather.js")
  println("Done")