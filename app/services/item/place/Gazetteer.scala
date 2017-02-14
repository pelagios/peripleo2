package services.item.place

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class Gazetteer(name: String)

object Gazetteer {

  implicit val gazetteerFormat: Format[Gazetteer] =
    Format(
      JsPath.read[String].map(Gazetteer(_)),
      Writes[Gazetteer](t => JsString(t.name))
    )
   
}