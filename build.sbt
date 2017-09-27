name := """peripleo2"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

scalacOptions ++= Seq("-feature")

resolvers ++= Seq(
  "Open Source Geospatial Foundation Repository" at "http://download.osgeo.org/webdav/geotools/",
  "http://maven.geotoolkit.org/" at "http://maven.geotoolkit.org/"
)

libraryDependencies ++= Seq(
  ws,
  cache,
  filters,

  "com.nrinaudo" %% "kantan.csv" % "0.1.15",
  "com.sksamuel.elastic4s" %% "elastic4s-streams" % "2.4.0",
  "com.vividsolutions" % "jts" % "1.13",
  "jp.t2v" %% "play2-auth" % "0.14.1",
  "org.geotools" % "gt-geojson" % "14.3",
  "org.webjars" %% "webjars-play" % "2.5.0",

  // Scalagios core + transient dependencies
  "org.pelagios" % "scalagios-core" % "2.0.3" from "https://github.com/pelagios/scalagios/releases/download/v2.0.3/scalagios-core_2.11-2.0.3.jar",
  "org.openrdf.sesame" % "sesame-rio-n3" % "2.7.5",
  "org.openrdf.sesame" % "sesame-rio-rdfxml" % "2.7.5",

  "org.webjars" % "d3js" % "4.2.1",
  "org.webjars" % "jquery" % "1.12.0",
  "org.webjars" % "jquery-ui" % "1.11.4",
  "org.webjars" % "leaflet" % "1.0.3",
  "org.webjars" % "numeral-js" % "1.5.3-1",
  "org.webjars" % "typeaheadjs" % "0.11.1",
  "org.webjars" % "velocity" % "1.1.0",
  "org.webjars.bower" % "timeago" % "1.5.3",
  "org.webjars.npm" % "jquery.scrollintoview" % "1.9.4",
  "org.webjars.npm" % "leaflet-iiif" % "1.2.1",
  "org.webjars.npm" % "scrollreveal" % "3.3.6",

  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
)

pipelineStages := Seq(rjs, digest, gzip)

includeFilter in (Assets, LessKeys.less) := "*.less"

excludeFilter in (Assets, LessKeys.less) := "_*.less"
