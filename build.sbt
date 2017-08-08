val scalaV = "2.12.2"


lazy val backend = (project in file("backend")).settings(
  scalaJSProjects := Seq(frontend),
  scalaVersion := scalaV,
  pipelineStages in Assets := Seq(scalaJSPipeline),
  pipelineStages := Seq(digest, gzip),
  routesImport += "extension._",
  libraryDependencies ++= Seq(
    "com.vmunier" %% "scalajs-scripts" % "1.1.1",
    "org.scalatestplus.play" %% "scalatestplus-play" % "3.0.0-M3" % Test,
    "com.softwaremill.macwire" %% "macros" % "2.3.0" % "provided",
    "com.typesafe.akka" % "akka-typed_2.12" % "2.5.3"
  ),
  libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.0"
).enablePlugins(PlayScala).dependsOn(sharedJvm)

lazy val frontend = (project in file("frontend")).settings(
  scalaJSUseMainModuleInitializer := false,
  scalaVersion := scalaV,
  libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % "0.9.1",
    "com.lihaoyi" %%% "scalatags" % "0.6.5",
    "com.typesafe.play" %%% "play-json" % "2.6.0"
  ),
  jsDependencies += RuntimeDOM
).enablePlugins(ScalaJSPlugin, ScalaJSWeb)
  .dependsOn(sharedJs)

lazy val shared = (crossProject.crossType(CrossType.Pure) in file("shared"))
  .settings(
    scalaVersion := scalaV,
    libraryDependencies ++= Seq(
      "io.monix"  %%% "monix" % "2.3.0",
      "io.suzaku" %%% "boopickle" % "1.2.6",
      "com.typesafe.play" %%% "play-json" % "2.6.0",
      "com.github.japgolly.scalacss" %%% "ext-scalatags" % "0.5.3",
      "com.github.japgolly.scalacss" %%% "core" % "0.5.3")
  ).jsConfigure(_ enablePlugins ScalaJSWeb)

lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js

onLoad in Global := (Command.process("project backend", _: State)) compose (onLoad in Global).value