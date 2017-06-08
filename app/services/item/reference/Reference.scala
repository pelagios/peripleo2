package services.item.reference

import com.vividsolutions.jts.geom.Envelope
import java.util.UUID
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import services.HasGeometry

case class Reference(
  parentUri     : String,
  referenceType : ReferenceType.Value,
  referenceTo   : ReferenceTo,
  relation      : Option[Relation.Value],
  homepage      : Option[String],
  context       : Option[String],
  depiction     : Option[ReferenceDepiction]
) {
  
  /** An unbound version of this reference **/
  lazy val unbind = UnboundReference(
    parentUri,
    referenceType,
    referenceTo.uri,
    relation,
    homepage,
    context,
    depiction)
    
  def rebind(destination: UUID, bbox: Option[Envelope]): Reference =
    this.copy(referenceTo = ReferenceTo(referenceTo.uri, destination, bbox))

}

case class ReferenceTo(uri: String, docId: UUID, bbox: Option[Envelope])

case class ReferenceDepiction(url: String, thumbnail: Option[String])

object Reference {

  implicit val referenceFormat: Format[Reference] = (
    (JsPath \ "parent_uri").format[String] and
    (JsPath \ "reference_type").format[ReferenceType.Value] and
    (JsPath \ "reference_to").format[ReferenceTo] and
    (JsPath \ "relation").formatNullable[Relation.Value] and
    (JsPath \ "homepage").formatNullable[String] and
    (JsPath \ "context").formatNullable[String] and
    (JsPath \ "depiction").formatNullable[ReferenceDepiction]
  )(Reference.apply, unlift(Reference.unapply))

}

object ReferenceTo extends HasGeometry {

  implicit val referenceToFormat: Format[ReferenceTo] = (
    (JsPath \ "uri").format[String] and
    (JsPath \ "doc_id").format[UUID] and
    (JsPath \ "bbox").formatNullable[Envelope]
  )(ReferenceTo.apply, unlift(ReferenceTo.unapply))

}

object ReferenceDepiction {

  implicit val referenceDepictionFormat: Format[ReferenceDepiction] = (
    (JsPath \ "url").format[String] and
    (JsPath \ "thumbnail").formatNullable[String]
  )(ReferenceDepiction.apply, unlift(ReferenceDepiction.unapply))

}
