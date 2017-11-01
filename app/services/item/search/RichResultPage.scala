package services.item.search

import com.vividsolutions.jts.geom.{ Coordinate, Envelope, Geometry }
import java.util.UUID
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import services.{ Aggregation, HasGeometry, HasNullableSeq, HasNullableBoolean }
import services.item.{ Item, ItemType, ItemRecord, TemporalBounds }
import services.item.reference.TopReferenced

case class RichResultPage(
    
  took: Long,
  
  total: Long,
  
  offset: Int,
  
  limit: Long,
  
  items: Seq[RichResultItem],
  
  aggregations: Seq[Aggregation],
  
  topReferenced: Option[TopReferenced]
  
)

object RichResultPage extends HasNullableSeq {

  /** JSON serialization **/
  implicit val richResultPageWrites: Writes[RichResultPage] = (
    (JsPath \ "took").write[Long] and
    (JsPath \ "total").write[Long] and
    (JsPath \ "offset").write[Int] and
    (JsPath \ "limit").write[Long] and
    (JsPath \ "items").write[Seq[RichResultItem]] and
    (JsPath \ "aggregations").writeNullable[Seq[Aggregation]]
      .contramap[Seq[Aggregation]](toOptSeq) and
    (JsPath \ "top_referenced").writeNullable[TopReferenced]
  )(unlift(RichResultPage.unapply))
  
}

case class RichResultItem(
  
  item : Item,
  
  score: Float,
  
  isHitOnReference: Boolean = false

)

object RichResultItem extends HasGeometry with HasNullableBoolean {
    
  implicit val richResultItemWrites: Writes[RichResultItem] = (
    (JsPath \ "doc_id").write[UUID] and
    (JsPath \ "match_score").write[Float] and
    (JsPath \ "item_type").write[ItemType] and
    (JsPath \ "title").write[String] and
    (JsPath \ "representative_geometry").writeNullable[Geometry] and
    (JsPath \ "representative_point").writeNullable[Coordinate] and
    (JsPath \ "bbox").writeNullable[Envelope] and
    (JsPath \ "temporal_bounds").writeNullable[TemporalBounds] and
    (JsPath \ "hit_on_reference").formatNullable[Boolean]
      .inmap[Boolean](fromOptBool, toOptBool) and
    (JsPath \ "is_conflation_of").write[Seq[ItemRecord]]
  )(r => (
     r.item.docId,
     r.score,
     r.item.itemType,
     r.item.title,
     r.item.representativeGeometry,
     r.item.representativePoint,
     r.item.bbox,
     r.item.temporalBounds,
     r.isHitOnReference,
     r.item.isConflationOf
  ))
  
}