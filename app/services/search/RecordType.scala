package services.search

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

object RecordType extends Enumeration {

  val DATASET = Value("DATASET")

  val OBJECT  = Value("OBJECT")
  
  val PERSON  = Value("PERSON")
  
  val PLACE   = Value("PLACE")
  
  val PERIOD  = Value("PERIOD")
  
  implicit val recordTypeFormat: Format[RecordType.Value] =
    Format(
      JsPath.read[JsString].map(json => RecordType.withName(json.value)),
      Writes[RecordType.Value](r => Json.toJson(r.toString))
    )

}