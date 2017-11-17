package controllers.api.legacy.response

import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.HasNullableSeq
import services.item.Item

case class LegacyPlace()

case class ReferencedIn(title: String, identifier: String, count: Long)
  
object LegacyPlace extends HasNullableSeq {
  
  def fromItem(i: Item, annotations: Long, uniquePlaces: Int) = {
    
  }
  
  /*
  implicit val legacyPlaceWrites: Writes[LegacyPlace] = (
    (JsPath \ "identifier").write[String] and
    (JsPath \ "title").write[String] and
    (JsPath \ "in_dataset").writeNullable[String] and
    (JsPath \ "homepage").writeNullable[String] and
    (JsPath \ "geo_bounds").writeNullable[LegacyGeoBounds] and
    (JsPath \ "depictions").writeNullable[Seq[String]]
      .contramap[Seq[String]](toOptSeq) and
    (JsPath \ "temporal_bounds").writeNullable[LegacyTemporalBounds] and
    (JsPath \ "num_annotations").write[Long] and
    (JsPath \ "num_unique_places").write[Int]
  )(unlift(LegacyPlace.unapply))
  */
  
}