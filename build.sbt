name := """peripleo2"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  jdbc,
  cache,

  "com.sksamuel.elastic4s" %% "elastic4s-streams" % "2.4.0",
  "com.vividsolutions" % "jts" % "1.13",
  "jp.t2v" %% "play2-auth" % "0.14.1",

  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
)
