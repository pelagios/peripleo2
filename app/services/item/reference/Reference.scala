package services.item.reference

import com.vividsolutions.jts.geom.Envelope
import java.util.UUID
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import services.{HasGeometry, HasNullableSeq}
import services.item.{Item, ItemType, PathHierarchy}

case class Reference(
  parentUri     : String,
  referenceTo   : ReferenceTo,
  relation      : Option[Relation.Value],
  homepage      : Option[String],
  quote         : Option[ReferenceQuote],
  depiction     : Option[ReferenceDepiction]
) {
  
  /** An unbound version of this reference **/
  lazy val unbind = UnboundReference(
    parentUri,
    referenceTo.uri,
    relation,
    homepage,
    quote,
    depiction)
    
  def rebind(item: Item) =
    this.copy(referenceTo = ReferenceTo(item.docId, referenceTo.uri, item.itemType, item.isConflationOf.flatMap(_.isPartOf), item.bbox))

}

case class ReferenceTo(
  docId: UUID,
  uri: String,
  itemType: ItemType,
  isPartOf: Seq[PathHierarchy],
  bbox: Option[Envelope])

case class ReferenceQuote(chars: String, context: Option[String], offset: Option[Int])

case class ReferenceDepiction(url: String, thumbnail: Option[String])

object Reference {

  implicit val referenceFormat: Format[Reference] = (
    (JsPath \ "parent_uri").format[String] and
    (JsPath \ "reference_to").format[ReferenceTo] and
    (JsPath \ "relation").formatNullable[Relation.Value] and
    (JsPath \ "homepage").formatNullable[String] and
    (JsPath \ "quote").formatNullable[ReferenceQuote] and
    (JsPath \ "depiction").formatNullable[ReferenceDepiction]
  )(Reference.apply, unlift(Reference.unapply))

}

object ReferenceTo extends HasGeometry with HasNullableSeq {

  implicit val referenceToFormat: Format[ReferenceTo] = (
    (JsPath \ "doc_id").format[UUID] and
    (JsPath \ "uri").format[String] and
    (JsPath \ "item_type").format[ItemType] and
    (JsPath \ "is_part_of").formatNullable[Seq[PathHierarchy]]
      .inmap[Seq[PathHierarchy]](fromOptSeq[PathHierarchy], toOptSeq[PathHierarchy]) and      
    (JsPath \ "bbox").formatNullable[Envelope]
  )(ReferenceTo.apply, unlift(ReferenceTo.unapply))

}

object ReferenceQuote {
  
  implicit val referenceQuoteFormat: Format[ReferenceQuote] = (
    (JsPath \ "chars").format[String] and
    (JsPath \ "context").formatNullable[String] and
    (JsPath \ "offset").formatNullable[Int]
  )(ReferenceQuote.apply, unlift(ReferenceQuote.unapply))
  
}

object ReferenceDepiction {

  implicit val referenceDepictionFormat: Format[ReferenceDepiction] = (
    (JsPath \ "url").format[String] and
    (JsPath \ "thumbnail").formatNullable[String]
  )(ReferenceDepiction.apply, unlift(ReferenceDepiction.unapply))

}
