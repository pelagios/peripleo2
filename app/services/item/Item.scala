package services.item

import com.vividsolutions.jts.geom.{ Coordinate, Envelope, Geometry, GeometryFactory }
import java.util.UUID
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import services.{ HasDate, HasGeometry, HasNullableSeq }

case class Item private[item] (
  docId                     : UUID,
  itemType                  : ItemType,
  title                     : String,
  representativeGeometry    : Option[Geometry],
  representativePoint       : Option[Coordinate],
  temporalBounds            : Option[TemporalBounds],
  isConflationOf            : Seq[ItemRecord],
  private val storedBBox    : Option[Envelope] = None
) {
  
  lazy val identifiers = isConflationOf.flatMap(_.identifiers)
  
  lazy val bbox = storedBBox match {
    case Some(env) => Some(env)
    case None => representativeGeometry.map(_.getEnvelopeInternal)
  }

  /*
  private[item] lazy val autocomplete = AutocompleteData(
    isConflationOf.map(_.title) ++ isConflationOf.flatMap(_.names.map(_.name)),
    identifiers.head,
    itemType,
    title,
    isConflationOf.flatMap(_.descriptions.map(_.description)).headOption)
  */

}

object Item extends HasGeometry {
  
  private val factory = new GeometryFactory()

  /** Per convention, first in list determines docId and top-level properties **/
  def fromRecords(docId: UUID, itemType: ItemType, records: Seq[ItemRecord]) = {

    val temporalBoundsUnion = records.flatMap(_.temporalBounds) match {
      case bounds if bounds.size > 0 => Some(TemporalBounds.computeUnion(bounds))
      case _ => None
    }

    // Helper to get the first defined of a list of options
    def getFirst[T](seq: Seq[Option[T]]) = seq.flatten.headOption
    
    // TODO we should be smarter about determining the 'representative' geometry
    // TODO cf. Recogito 2: model.place.GazetteerRecord.getPreferredLocation
    val firstGeometry = getFirst(records.map(_.geometry))
    val firstPoint = getFirst(records.map(_.representativePoint))
    
    // We need to make sure that either both point & geom are set, or none
    val (geom, point) = (firstGeometry, firstPoint) match {
      case (Some(g), Some(pt)) => (Some(g), Some(pt))
      case (Some(g), None)     => (Some(g), Some(g.getCentroid.getCoordinate))
      case (None, Some(pt))    => (Some(factory.createPoint(pt)), Some(pt))
      case (None, None)        => (None, None)
    }
    
    val mostDetailed = getMostDetailedRecord(records)

    Item(
      docId,
      itemType,
      mostDetailed.title,
      geom,
      point,
      temporalBoundsUnion,
      records
    )
  }

  /** Picks a 'most detailed' record we can use for representative metadata **/
  def getMostDetailedRecord(records: Seq[ItemRecord]) =
    // For the time being, we'll use number of names + number of SKOS matches as
    // a measure. In addition, we'll boost the score if the matches contain
    // a Wikidata or Wikipedia reference.
    records.sortBy { record =>
      val score = record.directMatches.size + record.names.size
      val boostWikidata = record.directMatches.contains("www.wikidata.org")
      val boostWikipedia = record.directMatches.contains("wikipedia.org")
      
      val boost = 
        if (boostWikidata && boostWikipedia) 1.44
        else if (boostWikidata || boostWikipedia) 1.2
        else 1
        
      // Sort descending
      - score * boost
    }.head
  
  /** Shorthand that creates an item from a single record **/
  def fromRecord(docId: UUID, itemType: ItemType, record: ItemRecord) =
    Item(
      docId,
      itemType,
      record.title,
      record.geometry,
      record.representativePoint,
      record.temporalBounds,
      Seq(record))

  // Although this means a bit more code, we'll keep a separate Reader and Writer
  // for the item, so we can keep the autocomplete data out of the case class and handle envelopes

  implicit val itemReads: Reads[Item] = (
    (JsPath \ "doc_id").read[UUID] and
    (JsPath \ "item_type").read[ItemType] and
    (JsPath \ "title").read[String] and
    (JsPath \ "representative_geometry").readNullable[Geometry] and
    (JsPath \ "representative_point").readNullable[Coordinate] and
    (JsPath \ "temporal_bounds").readNullable[TemporalBounds] and
    (JsPath \ "is_conflation_of").read[Seq[ItemRecord]] and
    (JsPath \ "bbox").readNullable[Envelope]
  )(Item.apply _)

  implicit val itemWrites: Writes[Item] = (
    (JsPath \ "doc_id").write[UUID] and
    (JsPath \ "item_type").write[ItemType] and
    (JsPath \ "title").write[String] and
    (JsPath \ "representative_geometry").writeNullable[Geometry] and
    (JsPath \ "representative_point").writeNullable[Coordinate] and
    (JsPath \ "bbox").writeNullable[Envelope] and
    (JsPath \ "temporal_bounds").writeNullable[TemporalBounds] and
    (JsPath \ "is_conflation_of").write[Seq[ItemRecord]]
    // (JsPath \ "suggest").write[AutocompleteData]
  )(item => (
      item.docId,
      item.itemType,
      item.title,
      item.representativeGeometry,
      item.representativePoint,
      item.bbox,
      item.temporalBounds,
      item.isConflationOf
      // item.autocomplete
  ))

}

/*
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
*/
