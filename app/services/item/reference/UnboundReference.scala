package services.item.reference

import java.util.UUID

/** A helper to handle references during ingest, where there's no known reference UUID yet **/
case class UnboundReference(

  referenceType: ReferenceType.Value,

  uri: String,

  relation: Option[Relation.Value],

  homepage: Option[String],

  context: Option[String],

  depiction: Option[ReferenceDepiction]

) {

  def toReference(docId: UUID) =
    Reference(referenceType, ReferenceTo(uri, docId), relation, homepage, context, depiction)

}
