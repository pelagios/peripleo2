package services.item.reference

import java.util.UUID
import services.item.ItemRecord
import com.vividsolutions.jts.geom.Envelope

/** A helper to handle references during ingest, where there's no known reference UUID yet **/
case class UnboundReference(
  parentUri     : String,
  referenceType : ReferenceType.Value,
  uri           : String,
  relation      : Option[Relation.Value],
  homepage      : Option[String],
  context       : Option[String],
  depiction     : Option[ReferenceDepiction]
) {
  
  lazy val normalize = this.copy(
    parentUri = ItemRecord.normalizeURI(parentUri),
    uri = ItemRecord.normalizeURI(uri))

  def toReference(docId: UUID, bbox: Option[Envelope]) =
    Reference(parentUri, referenceType, ReferenceTo(uri, docId, bbox), relation, homepage, context, depiction)

}
