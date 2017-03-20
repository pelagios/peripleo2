package services.item

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class Reference(

  referenceType: ReferenceType.Value,
  
  relation: Option[Relation.Value],
  
  uri: String,
  
  rootUri: String,
  
  homepage: Option[String],
  
  context: Option[String],
  
  depiction: Option[ReferenceDepiction]
    
)

case class ReferenceDepiction(
    
  url: String,
  
  thumbnail: Option[String]
    
)

object Reference {
  
  implicit val referenceFormat: Format[Reference] = (
    (JsPath \ "reference_type").format[ReferenceType.Value] and
    (JsPath \ "relation").formatNullable[Relation.Value] and
    (JsPath \ "uri").format[String] and
    (JsPath \ "root_uri").format[String] and
    (JsPath \ "homepage").formatNullable[String] and
    (JsPath \ "context").formatNullable[String] and
    (JsPath \ "depiction").formatNullable[ReferenceDepiction]
  )(Reference.apply, unlift(Reference.unapply))
  
}


object ReferenceDepiction {
  
  implicit val referenceDepictionFormat: Format[ReferenceDepiction] = (
    (JsPath \ "url").format[String] and
    (JsPath \ "thumbnail").formatNullable[String]
  )(ReferenceDepiction.apply, unlift(ReferenceDepiction.unapply))
  
}


