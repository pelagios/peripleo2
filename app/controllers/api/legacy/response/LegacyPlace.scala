package controllers.api.legacy.response

import com.vividsolutions.jts.geom.Geometry
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.{ HasNullableSeq, HasGeometry }
import services.item.Item

case class LegacyPlace(item: Item) {
  
  /** Shorthands **/
  
  val identifier = item.identifiers.head
  
  val description = item.isConflationOf.flatMap(_.descriptions.map(_.description)).head
  
  val names = item.isConflationOf.flatMap(_.names.flatMap(_.name.split(",").map(_.trim))).distinct
  
  val bounds = item.representativeGeometry.map(geom => LegacyGeoBounds.fromEnvelope(geom.getEnvelopeInternal))
  
  val matches = item.identifiers diff Seq(identifier)
  
}

case class ReferencedIn(title: String, identifier: String, count: Long)
  
object LegacyPlace extends HasNullableSeq with HasGeometry {
  
  implicit val legacyPlaceWrites: Writes[LegacyPlace] = (
    (JsPath \ "identifier").write[String] and
    (JsPath \ "title").write[String] and
    (JsPath \ "object_type").write[String] and
    (JsPath \ "description").write[String] and
    (JsPath \ "names").write[Seq[String]] and
    (JsPath \ "matches").write[Seq[String]] and
    (JsPath \ "geo_bounds").writeNullable[LegacyGeoBounds] and
    (JsPath \ "geometry").writeNullable[Geometry]
  )(p => (
      p.identifier,
      p.item.title,
      "Place",
      p.description,
      p.names,
      p.matches,
      p.bounds,
      p.item.representativeGeometry
  ))

}