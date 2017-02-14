package services.item

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

object ItemType extends Enumeration {

  val DATASET = Value("DATASET")

  val OBJECT  = Value("OBJECT")
  
  val PERSON  = Value("PERSON")
  
  val PLACE   = Value("PLACE")
  
  implicit val itemTypeFormat: Format[ItemType.Value] =
    Format(
      JsPath.read[JsString].map(json => ItemType.withName(json.value)),
      Writes[ItemType.Value](r => Json.toJson(r.toString))
    )

}