name := """peripleo2"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

scalacOptions ++= Seq("-feature")

libraryDependencies ++= Seq(
  ws,
  cache,
  filters,

  "com.sksamuel.elastic4s" %% "elastic4s-streams" % "2.4.0",
  "com.vividsolutions" % "jts" % "1.13",
  "jp.t2v" %% "play2-auth" % "0.14.1",
  "org.geotools" % "gt-geojson" % "14.3",
  "org.webjars" %% "webjars-play" % "2.5.0",

  // Scalagios core + transient dependencies
  "org.pelagios" % "scalagios-core" % "2.0.1" from "https://github.com/pelagios/scalagios/releases/download/v2.0.1/scalagios-core.jar",
  "org.openrdf.sesame" % "sesame-rio-n3" % "2.7.5",
  "org.openrdf.sesame" % "sesame-rio-rdfxml" % "2.7.5",

  "org.webjars" % "jquery" % "1.12.0",
  "org.webjars" % "leaflet" % "1.0.3",
  "org.webjars" % "velocity" % "1.1.0",

  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
)

includeFilter in (Assets, LessKeys.less) := "*.less"

excludeFilter in (Assets, LessKeys.less) := "_*.less"
