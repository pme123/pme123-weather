#!/usr/bin/env -S scala shebang

//> using dep "com.lihaoyi::os-lib:0.11.3"


@main
def main(args: String*) =
  val proc = "full"
  println(s"Running ${proc}LinkJS")
  os.proc("sbt", s"${proc}LinkJS").call()
  os.proc("npm", "run", "build").call()
  println("Adjusting file")
  val indexPath = os.pwd / "dist" / "index.html"
  val index = os.read(indexPath)
  os.write.over(indexPath, index.replace("\"/assets/", "\"assets/"))
  os.remove.all(os.pwd / "docs")
  os.copy(os.pwd / "dist", os.pwd / "docs")
  //os.copy.over(os.pwd / "target" / "scala-3.6.2" / s"pme123-weather-${proc.replace("full", "")}opt.js", os.pwd / "pme123-weather.js")
  println("Done")