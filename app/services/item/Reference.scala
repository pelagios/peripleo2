package services.item

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class Reference(
    
  referenceType: ReferenceType.Value,
  
  relation: Option[Relation.Value],
  
  uri: Option[String],
  
  context: Option[String]
    
)

object Reference {
  
  implicit val referenceFormat: Format[Reference] = (
    (JsPath \ "reference_type").format[ReferenceType.Value] and
    (JsPath \ "relation").formatNullable[Relation.Value] and
    (JsPath \ "uri").formatNullable[String] and
    (JsPath \ "context").formatNullable[String]
  )(Reference.apply, unlift(Reference.unapply))
  
}

