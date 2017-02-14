package services.item

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class Description(description: String, language: Option[Language] = None)

object Description {

  implicit val descriptionFormat: Format[Description] = (
    (JsPath \ "description").format[String] and
    (JsPath \ "language").formatNullable[Language]
  )(Description.apply, unlift(Description.unapply))

}