package services.search

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class Category(label: String, uri: Option[String] = None)

object Category {
 
  implicit val categoryFormat: Format[Category] = (
    (JsPath \ "label").format[String] and
    (JsPath \ "uri").formatNullable[String]
  )(Category.apply, unlift(Category.unapply))
  
}