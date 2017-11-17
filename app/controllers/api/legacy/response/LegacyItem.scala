package controllers.api.legacy.response

import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.HasNullableSeq
import services.item.Item

case class LegacyItem(
  identifier      : String,
  title           : String,
  inDataset       : Option[String],
  homepage        : Option[String],
  geoBounds       : Option[LegacyGeoBounds],
  depictions      : Seq[String],
  temporalBounds  : Option[LegacyTemporalBounds],
  numAnnotations  : Long,
  numUniquePlaces : Int)
  
object LegacyItem extends HasNullableSeq {
  
  def fromItem(i: Item, annotations: Long, uniquePlaces: Int) = {
    val firstRecord = i.isConflationOf.head
    LegacyItem(
      firstRecord.uri,
      i.title,
      firstRecord.isInDataset.map(_.ids.last),
      firstRecord.homepage,
      i.bbox.map(LegacyGeoBounds.fromEnvelope(_)),
      i.isConflationOf.flatMap(_.depictions.map(_.url)),
      i.temporalBounds.map(LegacyTemporalBounds.fromBounds(_)),
      annotations, uniquePlaces)
  }
  
  implicit val legacyItemWrites: Writes[LegacyItem] = (
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
  )(unlift(LegacyItem.unapply))
  
}