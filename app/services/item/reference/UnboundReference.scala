package services.item.reference

import java.util.UUID
import services.item.{Item, ItemRecord}

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

  def toReference(item: Item) =
    Reference(parentUri, ReferenceTo(item.docId, uri, item.itemType, item.isConflationOf.flatMap(_.isPartOf), item.bbox), relation, homepage, quote, depiction)

}
