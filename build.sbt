name := """peripleo2"""

version := "2.2"

scalaVersion := "2.11.11"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalacOptions ++= Seq("-feature")

updateOptions := updateOptions.value.withGigahorse(false)

resolvers ++= Seq(
  "Typesafe" at "http://repo.typesafe.com/typesafe/releases/",
  "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
  "Geotools" at "https://repo.osgeo.org/repository/geotools-releases/",
  "Geotoolkit" at "http://maven.geotoolkit.org",
  "Boundless" at "https://repo.osgeo.org/repository/Geoserver-releases/",
  "Atlassian Releases" at "https://maven.atlassian.com/public/",
  "Atlassian Maven" at "https://maven.atlassian.com/content/repositories/atlassian-public/",
  "mvnrepository" at "http://mvnrepository.com/artifact",
  Resolver.jcenterRepo 
)

libraryDependencies ++= Seq(
  ws,
  ehcache,
  filters,
  guice,

  "com.mohiva" %% "play-silhouette" % "5.0.7",
  "com.mohiva" %% "play-silhouette-password-bcrypt" % "5.0.7",
  "com.mohiva" %% "play-silhouette-crypto-jca" % "5.0.7",
  "com.mohiva" %% "play-silhouette-persistence" % "5.0.7",
  "com.mohiva" %% "play-silhouette-testkit" % "5.0.7" % Test,
  "com.mohiva" %% "play-silhouette-cas" % "5.0.7",

  "com.nrinaudo" %% "kantan.csv" % "0.2.1",
  "com.nrinaudo" %% "kantan.csv-commons" % "0.2.1",

  "com.sksamuel.elastic4s" %% "elastic4s-core" % "5.6.1",
  "com.sksamuel.elastic4s" %% "elastic4s-tcp" % "5.6.1",

  "com.vividsolutions" % "jts-core" % "1.14.0",

  "eu.bitwalker" % "UserAgentUtils" % "1.20",

  "net.codingwell" %% "scala-guice" % "4.1.1",

  "org.apache.logging.log4j" % "log4j-to-slf4j" % "2.8.2",

  "org.geotools" % "gt-geojson" % "18.1",

  "org.webjars" %% "webjars-play" % "2.6.2",

  // Scalagios core + transient dependencies
  "org.pelagios" % "scalagios-core" % "2.0.7" from "https://github.com/pelagios/scalagios/releases/download/v2.0.7/scalagios-core_2.11-2.0.7.jar",
  "org.openrdf.sesame" % "sesame-rio-n3" % "2.8.5",
  "org.openrdf.sesame" % "sesame-rio-rdfxml" % "2.8.5",
  "org.openrdf.sesame" % "sesame-rio-turtle" % "2.8.5",
  "org.openrdf.sesame" % "sesame-rio-jsonld" % "2.8.5",
  "com.github.jsonld-java" % "jsonld-java" % "0.12.0",

  "org.webjars" % "d3js" % "4.2.1",
  "org.webjars" % "jquery" % "1.12.0",
  "org.webjars" % "jquery-ui" % "1.11.4",
  "org.webjars" % "leaflet" % "1.0.3",
  "org.webjars" % "numeral-js" % "1.5.3-1",
  "org.webjars" % "typeaheadjs" % "0.11.1",
  "org.webjars" % "velocity" % "1.1.0",
  "org.webjars.bower" % "timeago" % "1.5.3" intransitive(),
  "org.webjars.npm" % "jquery.scrollintoview" % "1.9.4" intransitive(),
  "org.webjars.npm" % "leaflet-iiif" % "1.2.1",
  "org.webjars.npm" % "scrollreveal" % "3.3.6",

  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
)

pipelineStages := Seq(rjs, digest, gzip)

includeFilter in (Assets, LessKeys.less) := "*.less"

excludeFilter in (Assets, LessKeys.less) := "_*.less"
