package services.item

import com.vividsolutions.jts.geom.{ Coordinate, Geometry }
import java.util.UUID
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import services.{ HasDate, HasGeometry, HasNullableSeq }

case class Item private[item] (

  docId: UUID,

  itemType: ItemType,

  title: String,

  geometry: Option[Geometry],

  representativePoint: Option[Coordinate],

  temporalBounds: Option[TemporalBounds],

  isConflationOf: Seq[ItemRecord]

) {

  lazy val identifiers = isConflationOf.flatMap(_.identifiers)

  private[item] lazy val autocomplete = AutocompleteData(
    isConflationOf.map(_.title) ++ isConflationOf.flatMap(_.names.map(_.name)),
    identifiers.head,
    itemType,
    title,
    isConflationOf.flatMap(_.descriptions.map(_.description)).headOption)

}

object Item extends HasGeometry {

  import PathHierarchy._

  /** Per convention, first in list determines docId and top-level properties **/
  def fromRecords(itemType: ItemType, records: Seq[ItemRecord]) = {

    val temporalBoundsUnion = records.flatMap(_.temporalBounds) match {
      case bounds if bounds.size > 0 => Some(TemporalBounds.computeUnion(bounds))
      case _ => None
    }

    // Helper to get the first defined of a list of options

    def getFirst[T](seq: Seq[Option[T]]) = seq.flatten.headOption

    Item(
      UUID.randomUUID,
      itemType,
      records.head.title,
      getFirst(records.map(_.geometry)),
      getFirst(records.map(_.representativePoint)),
      temporalBoundsUnion,
      records
    )
  }

  /** Shorthand that creates an item from a single record **/
  def fromRecord(itemType: ItemType, record: ItemRecord) =
    Item(
      UUID.randomUUID,
      itemType,
      record.title,
      record.geometry,
      record.representativePoint,
      record.temporalBounds,
      Seq(record))

  // Although this means a bit more code, we'll keep a separate Reader and Writer
  // for the item, so we can keep the autocomplete data out of the case class.

  implicit val itemRead: Reads[Item] = (
    (JsPath \ "doc_id").read[UUID] and
    (JsPath \ "item_type").read[ItemType] and
    (JsPath \ "title").read[String] and
    (JsPath \ "geometry").readNullable[Geometry] and
    (JsPath \ "representative_point").readNullable[Coordinate] and
    (JsPath \ "temporal_bounds").readNullable[TemporalBounds] and
    (JsPath \ "is_conflation_of").read[Seq[ItemRecord]]
  )(Item.apply _)

  implicit val itemWrites: Writes[Item] = (
    (JsPath \ "doc_id").write[UUID] and
    (JsPath \ "item_type").write[ItemType] and
    (JsPath \ "title").write[String] and
    (JsPath \ "geometry").writeNullable[Geometry] and
    (JsPath \ "representative_point").writeNullable[Coordinate] and
    (JsPath \ "temporal_bounds").writeNullable[TemporalBounds] and
    (JsPath \ "is_conflation_of").write[Seq[ItemRecord]] and
    (JsPath \ "suggest").write[AutocompleteData]
  )(item => (
      item.docId,
      item.itemType,
      item.title,
      item.geometry,
      item.representativePoint,
      item.temporalBounds,
      item.isConflationOf,
      item.autocomplete
  ))

}

private[item] case class AutocompleteData private(input: Seq[String], output: String, payload: AutocompleteData.Payload)

private[item] object AutocompleteData {

  case class Payload(identifier: String, itemType: ItemType, description: Option[String])

  def apply(input: Seq[String], identifier: String, itemType: ItemType,
    title: String, description : Option[String]): AutocompleteData =
      AutocompleteData(input, title, Payload(identifier, itemType, description))

  // https://www.elastic.co/guide/en/elasticsearch/reference/2.4/search-suggesters-completion.html#indexing
  implicit val payloadWrites: Writes[Payload] = (
    (JsPath \ "identifier").write[String] and
    (JsPath \ "type").write[ItemType] and
    (JsPath \ "description").writeNullable[String]
  )(unlift(Payload.unapply))

  implicit val autocompleteDataWrites: Writes[AutocompleteData] = (
    (JsPath \ "input").write[Seq[String]] and
    (JsPath \ "output").write[String] and
    (JsPath \ "payload").write[Payload]
  )(unlift(AutocompleteData.unapply))

}
