#!/usr/bin/env -S scala shebang

//> using dep "com.lihaoyi::os-lib:0.11.3"


@main
def main(args: String*) =
  val proc = "full"
  println(s"Running ${proc}LinkJS")
  os.proc("sbt", s"${proc}LinkJS").call()
  os.proc("npm", "run", "build").call()
  println("Copy result to root")
  os.copy.over(os.pwd / "target" / "scala-3.6.2" / s"pme123-weather-${proc.replace("full", "")}opt.js", os.pwd / "pme123-weather.js")
  println("Done")