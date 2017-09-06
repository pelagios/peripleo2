package services.item

import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import services.HasDate

case class Depiction(
  url       : String,
  thumbnail : Option[String]   = None,
  iiifUri   : Option[String]   = None,
  caption   : Option[String]   = None,
  creator   : Option[String]   = None,
  createdAt : Option[DateTime] = None,
  license   : Option[String]   = None)

object Depiction extends HasDate {
  
  implicit val depictionFormat: Format[Depiction] = (
    (JsPath \ "url").format[String] and
    (JsPath \ "thumbnail").formatNullable[String] and
    (JsPath \ "iiif_uri").formatNullable[String] and
    (JsPath \ "caption").formatNullable[String] and
    (JsPath \ "creator").formatNullable[String] and
    (JsPath \ "created_at").formatNullable[DateTime] and
    (JsPath \ "license").formatNullable[String]
  )(Depiction.apply, unlift(Depiction.unapply))
  
}
