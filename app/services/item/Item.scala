package services.item

import com.vividsolutions.jts.geom.{ Coordinate, Geometry }
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import services.{ HasDate, HasGeometry, HasNullableSeq }

case class Item(
    
  identifiers: Seq[String],
  
  itemType: ItemType.Value,
  
  lastSyncedAt: Option[DateTime],

  lastChangedAt: Option[DateTime],
  
  categories: Seq[Category],
  
  title: String,
  
  isInDataset: Seq[PathHierarchy],
  
  isPartOf: Option[PathHierarchy],
  
  descriptions: Seq[Description],
  
  homepage: Option[String],
  
  license: Option[String],
  
  languages: Seq[Language],
  
  geometry: Option[Geometry],
  
  representativePoint: Option[Coordinate],
    
  temporalBounds: Option[TemporalBounds],
    
  periods: Seq[String],
  
  depictions: Seq[Depiction]
  
)

object Item extends HasDate with HasNullableSeq with HasGeometry {
  
  import PathHierarchy._
  
  implicit val itemFormat: Format[Item] = (
    (JsPath \ "identifiers").format[Seq[String]] and
    (JsPath \ "item_type").format[ItemType.Value] and
    (JsPath \ "last_synced_at").formatNullable[DateTime] and
    (JsPath \ "last_changed_at").formatNullable[DateTime] and
    (JsPath \ "categories").formatNullable[Seq[Category]]
      .inmap[Seq[Category]](fromOptSeq[Category], toOptSeq[Category]) and
    (JsPath \ "title").format[String] and
    (JsPath \ "is_in_dataset").formatNullable[Seq[String]]
      .inmap[Seq[PathHierarchy]](toHierarchies, fromHierarchies) and
    (JsPath \ "is_part_of").formatNullable[Seq[String]]
      .inmap[Option[PathHierarchy]](toHierarchy, fromHierarchy) and
    (JsPath \ "descriptions").formatNullable[Seq[Description]]
      .inmap[Seq[Description]](fromOptSeq[Description], toOptSeq[Description]) and
    (JsPath \ "homepage").formatNullable[String] and
    (JsPath \ "license").formatNullable[String] and
    (JsPath \ "languages").formatNullable[Seq[Language]]
      .inmap[Seq[Language]](fromOptSeq[Language], toOptSeq[Language]) and
    (JsPath \ "geometry").formatNullable[Geometry] and
    (JsPath \ "representative_point").formatNullable[Coordinate] and
    (JsPath \ "temporal_bounds").formatNullable[TemporalBounds] and
    (JsPath \ "periods").formatNullable[Seq[String]]
      .inmap[Seq[String]](fromOptSeq[String], toOptSeq[String]) and
    (JsPath \ "depictions").formatNullable[Seq[Depiction]]
      .inmap[Seq[Depiction]](fromOptSeq[Depiction], toOptSeq[Depiction])
  )(Item.apply, unlift(Item.unapply))
  
}
