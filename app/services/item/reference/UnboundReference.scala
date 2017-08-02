package services.item.reference

import java.util.UUID
import services.item.{ ItemType, ItemRecord }
import com.vividsolutions.jts.geom.Envelope

/** A helper to handle references during ingest, where there's no known reference UUID yet **/
case class UnboundReference(
  parentUri     : String,
  uri           : String,
  relation      : Option[Relation.Value],
  homepage      : Option[String],
  quote         : Option[ReferenceQuote],
  depiction     : Option[ReferenceDepiction]
) {
  
  lazy val normalize = this.copy(
    parentUri = ItemRecord.normalizeURI(parentUri),
    uri = ItemRecord.normalizeURI(uri))

  def toReference(docId: UUID, itemType: ItemType, bbox: Option[Envelope]) =
    Reference(parentUri, ReferenceTo(docId, uri, itemType, bbox), relation, homepage, quote, depiction)

}
