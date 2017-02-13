package services.search

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

object ReferenceType extends Enumeration {

  val PLACE = Value("PLACE")
  
  val PERSON = Value("PERSON")
    
  implicit val referenceTypeFormat: Format[ReferenceType.Value] =
    Format(
      JsPath.read[JsString].map(json => ReferenceType.withName(json.value)),
      Writes[ReferenceType.Value](r => Json.toJson(r.toString))
    )

}