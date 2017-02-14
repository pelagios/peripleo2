package services.item

import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import services.HasDate
import services.HasNullableSeq

case class Item(
    
  identifiers: Seq[String],
  
  itemType: ItemType.Value,
  
  lastSyncedAt: Option[DateTime],

  lastChangedAt: Option[DateTime],
  
  categories: Seq[Category],
  
  title: String,
  
  isInDataset: Option[PathHierarchy],
  
  isPartOf: Option[PathHierarchy],
  
  descriptions: Seq[Description],
  
  homepage: Option[String],
  
  languages: Seq[Language],
    
  temporalBounds: Option[TemporalBounds],
    
  periods: Seq[String],
  
  depictions: Seq[Depiction]
  
)

object Item extends HasDate with HasNullableSeq {
  
  implicit val itemFormat: Format[Item] = (
    (JsPath \ "identifiers").format[Seq[String]] and
    (JsPath \ "item_type").format[ItemType.Value] and
    (JsPath \ "last_synced_at").formatNullable[DateTime] and
    (JsPath \ "last_changed_at").formatNullable[DateTime] and
    (JsPath \ "categories").formatNullable[Seq[Category]]
      .inmap[Seq[Category]](fromOptSeq[Category], toOptSeq[Category]) and
    (JsPath \ "title").format[String] and
    (JsPath \ "is_in_dataset").formatNullable[PathHierarchy] and
    (JsPath \ "is_part_of").formatNullable[PathHierarchy] and
    (JsPath \ "descriptions").formatNullable[Seq[Description]]
      .inmap[Seq[Description]](fromOptSeq[Description], toOptSeq[Description]) and
    (JsPath \ "homepage").formatNullable[String] and
    (JsPath \ "languages").formatNullable[Seq[Language]]
      .inmap[Seq[Language]](fromOptSeq[Language], toOptSeq[Language]) and
    (JsPath \ "temporal_bounds").formatNullable[TemporalBounds] and
    (JsPath \ "periods").formatNullable[Seq[String]]
      .inmap[Seq[String]](fromOptSeq[String], toOptSeq[String]) and
    (JsPath \ "depictions").formatNullable[Seq[Depiction]]
      .inmap[Seq[Depiction]](fromOptSeq[Depiction], toOptSeq[Depiction])
  )(Item.apply, unlift(Item.unapply))
  
}
