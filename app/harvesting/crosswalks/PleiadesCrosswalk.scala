package harvesting.crosswalks

import com.vividsolutions.jts.geom.{ Coordinate, Geometry }
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import services.{ HasDate, HasGeometry }
import services.item._

object PleiadesCrosswalk extends BaseGeoJSONCrosswalk {

  private val PLEIADES = PathHierarchy("pleiades", "pleiades")

  private def computeTemporalBounds(names: Seq[PleiadesName]): Option[TemporalBounds] = {
    val startDate= names.flatMap(_.startDate)
    val endDate = names.flatMap(_.endDate)
    if (startDate.isEmpty || endDate.isEmpty)
      None
    else
      Some(TemporalBounds.fromYears(startDate.min, endDate.max))
  }

  def fromJson(record: String): Option[ItemRecord] = super.fromJson[PleiadesRecord](record, { pleiades =>
    val names = pleiades.names.flatMap(_.toNames)
    ItemRecord(
      ItemRecord.normalizeURI(pleiades.uri),
      Seq(ItemRecord.normalizeURI(pleiades.uri)),
      DateTime.now(),
      pleiades.history.headOption.map(_.modified),
      pleiades.title,
      Some(PLEIADES),
      None, // isPartOf
      pleiades.placeTypes.map(Category(_)),
      pleiades.description.map(d => Seq(new Description(d))).getOrElse(Seq.empty[Description]),
      None, // homepage
      None, // license
      names.flatMap(_.language).distinct,
      Seq.empty[Depiction],
      pleiades.features.headOption.flatMap(g => Option(g.geometry)), // TODO compute union?
      pleiades.representativePoint,
      computeTemporalBounds(pleiades.names), // TODO temporalBounds
      names,
      Seq.empty[Link],
      None, None
    )
  })

}

case class HistoryRecord(modified: DateTime)

object HistoryRecord extends HasDate {

  implicit val historyRecordReads: Reads[HistoryRecord] = (JsPath \ "modified").read[DateTime].map(HistoryRecord(_))

}

case class PleiadesName(
  attested: Option[String],
  romanized: Option[String],
  language: Option[String],
  startDate: Option[Int],
  endDate: Option[Int]
) {

  // BAD bad Pleiades!
  lazy val normalizedLanguage = language match {
    case Some(language) if !language.trim.isEmpty => Some(language)
    case _ => None
  }

  lazy val toNames = Seq(attested, romanized).flatten.filter(!_.trim.isEmpty).map(Name(_, normalizedLanguage.flatMap(Language.safeParse)))

}

object PleiadesName {

   implicit val pleiadesNameReads: Reads[PleiadesName] = (
    (JsPath \ "attested").readNullable[String] and
    (JsPath \ "romanized").readNullable[String] and
    (JsPath \ "language").readNullable[String] and
    (JsPath \ "start").readNullable[Int] and
    (JsPath \ "end").readNullable[Int]
  )(PleiadesName.apply _)

}

case class PleiadesRecord(
  uri                 : String,
  history             : Seq[HistoryRecord],
  title               : String,
  description         : Option[String],
  names               : Seq[PleiadesName],
  features            : Seq[Feature],
  representativePoint : Option[Coordinate],
  placeTypes          : Seq[String])

object PleiadesRecord extends HasGeometry {

  implicit val pleiadesRecordReads: Reads[PleiadesRecord] = (
    (JsPath \ "uri").read[String] and
    (JsPath \ "history").read[Seq[HistoryRecord]] and
    (JsPath \ "title").read[String] and
    (JsPath \ "description").readNullable[String] and
    (JsPath \ "names").readNullable[Seq[PleiadesName]].map(_.getOrElse(Seq.empty[PleiadesName])) and
    (JsPath \ "features").readNullable[Seq[Feature]].map(_.getOrElse(Seq.empty[Feature])) and
    (JsPath \ "reprPoint").readNullable[Coordinate] and
    (JsPath \ "placeTypes").readNullable[Seq[String]].map(_.getOrElse(Seq.empty[String]))
  )(PleiadesRecord.apply _)

}
