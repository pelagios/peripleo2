package services.item

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

object Relation extends Enumeration {

  val ATTESTATION = Value("ATTESTATION")
  
  val COVERAGE = Value("COVERAGE")

  val FINDSPOT = Value("FINDSPOT")
    
  implicit val relationFormat: Format[Relation.Value] =
    Format(
      JsPath.read[JsString].map(json => Relation.withName(json.value)),
      Writes[Relation.Value](r => Json.toJson(r.toString))
    )

}