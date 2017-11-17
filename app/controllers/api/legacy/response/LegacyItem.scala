package controllers.api.legacy.response

import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.Page
import services.item.{Item, ItemType}
import services.item.search.{RichResultPage, RichResultItem}
import services.HasNullableSeq


/** Emulates the response format of the Peripleo v.1 API
  * 
  * Responses had a format like this:
  * 
  * {
  *   "identifier" : "bb4e2f4b0bc7f4d6c065cb5167f4d3f831ccf795af0204f2647f8ec1bbcabcba",
  *   "title" : "Periplus of the Euxine Sea",
  *   "object_type" : "Item",
  *   "temporal_bounds" : {
  *     "start" : 130,
  *     "end" : 130
  *   },
  *   "geo_bounds" : {
  *     "min_lon" : 23.7195,
  *     "max_lon" : 44.0,
  *     "min_lat" : 37.5197,
  *     "max_lat" : 45.5
  *   }
  * }
  * 
  */
case class LegacyItem(
  identifier     : String,
  title          : String,
  objectType     : String,
  homepage       : Option[String],
  datasetPath    : Seq[LegacyPathSegment],
  depictions     : Seq[String],
  temporalBounds : Option[LegacyTemporalBounds],
  geobounds      : Option[LegacyGeoBounds],
  names          : Seq[String],
  matches        : Seq[String])
  
object LegacyItem extends HasNullableSeq {
  
  private def getType(t: ItemType): String = t match {
    case ItemType.PLACE   => "Place"
    case ItemType.OBJECT  => "Item"
    case ItemType.FEATURE => "Item"
    case ItemType.PERSON  => "Person"
    case ItemType.PERIOD  => "Period"
    case _                => "Dataset"
  }
  
  def fromItem(r: RichResultItem) = {
    val firstRecord = r.item.isConflationOf.head
    
    val matches = {
      r.item.isConflationOf.flatMap(_.identifiers) ++
      r.item.isConflationOf.flatMap(_.links.map(_.uri))
    }.distinct diff Seq(firstRecord.uri)
    
    LegacyItem(
      firstRecord.uri,
      r.item.title,
      getType(r.item.itemType),
      firstRecord.homepage,
      firstRecord.isInDataset.map(h => LegacyDatasetPath.fromHierarchy(h)).getOrElse(Seq()),
      r.item.isConflationOf.flatMap(_.depictions.map(_.url)),
      r.item.temporalBounds.map(LegacyTemporalBounds.fromBounds(_)),
      r.item.bbox.map(LegacyGeoBounds.fromEnvelope(_)),
      r.item.isConflationOf.flatMap(_.names.map(_.name)).distinct,
      matches)
  }
  
  implicit val legacyItemWrites: Writes[LegacyItem] = (
    (JsPath \ "identifier").write[String] and
    (JsPath \ "title").write[String] and
    (JsPath \ "object_type").write[String] and
    (JsPath \ "homepage").writeNullable[String] and
    (JsPath \ "dataset_path").write[Seq[LegacyPathSegment]] and
    (JsPath \ "depictions").writeNullable[Seq[String]]
      .contramap[Seq[String]](toOptSeq) and
    (JsPath \ "temporal_bounds").writeNullable[LegacyTemporalBounds] and
    (JsPath \ "geo_bounds").writeNullable[LegacyGeoBounds] and
    (JsPath \ "names").writeNullable[Seq[String]]
      .contramap[Seq[String]](toOptSeq) and
    (JsPath \ "matches").writeNullable[Seq[String]]
      .contramap[Seq[String]](toOptSeq)
  )(unlift(LegacyItem.unapply))
  
}