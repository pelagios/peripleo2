package harvesting.crosswalks.people

import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.HasNullableSeq
import services.item._

/** Simple crosswalk for people data. Not a data crosswalk for simple people. **/
object SimplePeopleCrosswalk {
  
  private def toItem(record: SimplePeopleRecord, dataset: PathHierarchy): ItemRecord =
    ItemRecord(
      record.uri,
      Seq(record.uri),
      DateTime.now,
      None, // lastChangedAt
      record.title,
      Some(dataset),
      None, // isPartOf
      Seq.empty[Category],
      record.descriptions,
      None, None, // homepage, license
      Seq.empty[Language],
      record.depiction.map(uri => Seq(Depiction(uri))).getOrElse(Seq.empty[Depiction]),
      None, None, // geometry, representativePoint
      record.temporalBounds,
      record.names,
      record.closeMatches.map(uri => Link(uri, LinkType.CLOSE_MATCH)),
      None)
  
  def fromJson(dataset: PathHierarchy)(record: String): Option[ItemRecord] = {
    Json.fromJson[SimplePeopleRecord](Json.parse(record)) match {
      case s: JsSuccess[SimplePeopleRecord] =>
        Some(toItem(s.get, dataset))
        
      case e: JsError =>
        Logger.error(e.toString)      
        None
    }
  }
  
}

case class SimplePeopleRecord(
  uri            : String,
  title          : String,
  descriptions   : Seq[Description],
  names          : Seq[Name],
  depiction      : Option[String],
  temporalBounds : Option[TemporalBounds],
  closeMatches   : Seq[String])

object SimplePeopleRecord {

  implicit val simplePeopleRecordReads: Reads[SimplePeopleRecord] = (
    (JsPath \ "uri").read[String] and
    (JsPath \ "title").read[String] and
    (JsPath \ "descriptions").readNullable[Seq[Description]].map(_.getOrElse(Seq.empty[Description])) and
    (JsPath \ "names").readNullable[Seq[Name]].map(_.getOrElse(Seq.empty[Name])) and
    (JsPath \ "depiction").readNullable[String] and
    (JsPath \ "temporal_bounds").readNullable[TemporalBounds] and
    (JsPath \ "closeMatches").read[Seq[String]]
  )(SimplePeopleRecord.apply _)

}
