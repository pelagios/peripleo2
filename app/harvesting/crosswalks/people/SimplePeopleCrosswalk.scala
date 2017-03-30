package harvesting.crosswalks.people

import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.HasNullableSeq
import services.item._

/** Simple crosswalk for data about people. Not a crosswalk for data about simple people. **/
object SimplePeopleCrosswalk {
  
  private def toItem(record: SimplePeopleRecord, dataset: PathHierarchy): ItemRecord =
    ItemRecord(
      record.uri,
      record.identifiers,
      DateTime.now,
      None, // lastChangedAt
      record.title,
      Some(dataset),
      None, // isPartOf
      Seq.empty[Category],
      record.descriptions,
      None, None, // homepage, license
      Seq.empty[Language],
      Seq.empty[Depiction],
      None, None, // geometry, representativePoint
      None, // TODO temporalBounds
      record.names,
      Seq.empty[String], Seq.empty[String])
  
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
  identifiers    : Seq[String],
  title          : String,
  descriptions   : Seq[Description],
  names          : Seq[Name])

object SimplePeopleRecord {

  implicit val simplePeopleRecordReads: Reads[SimplePeopleRecord] = (
    (JsPath \ "uri").read[String] and
    (JsPath \ "identifiers").read[Seq[String]] and
    (JsPath \ "title").read[String] and
    (JsPath \ "descriptions").readNullable[Seq[Description]].map(_.getOrElse(Seq.empty[Description])) and
    (JsPath \ "names").readNullable[Seq[Name]].map(_.getOrElse(Seq.empty[Name]))
  )(SimplePeopleRecord.apply _)

}