ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.6.2"

lazy val plotlyJs = "org.webjars.bower" % "plotly.js" % "1.54.1"

lazy val root = (project in file("."))
  .settings(
    name := "pme123-weather",
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= Seq(
      "com.raquo" %%% "laminar" % "17.1.0", // Laminar library for Scala.js
      "com.softwaremill.sttp.client3" %%% "core" % "3.9.7",
      "com.softwaremill.sttp.client3" %%% "circe" % "3.9.7",
      "io.circe" %%% "circe-core" % "0.14.7",
      "io.circe" %%% "circe-generic" % "0.14.9",
      "io.circe" %%% "circe-parser" % "0.14.9",
      ("org.plotly-scala" %%% "plotly-render" % "0.8.5")
        .cross(CrossVersion.for3Use2_13)
        .exclude("org.scala-js", "scalajs-dom_sjs1_2.13") // Plotly for charting
    ),
    jsDependencies ++= Seq(
      plotlyJs
        .intransitive()
        ./("plotly.min.js")
        .commonJSName("Plotly")
    )
  ).enablePlugins(JSDependenciesPlugin, ScalaJSPlugin)
