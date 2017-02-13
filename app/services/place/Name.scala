package services.place

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import services.HasNullableBoolean
import services.search.Language

case class Name(name: String, language: Option[Language] = None, isTransliterated: Boolean = false, isHistoric: Boolean = false)

object Name extends HasNullableBoolean {

  implicit val literalFormat: Format[Name] = (
    (JsPath \ "name").format[String] and
    (JsPath \ "language").formatNullable[Language] and
    (JsPath \ "is_romanized").formatNullable[Boolean]
      .inmap[Boolean](fromOptBool, toOptBool) and
    (JsPath \ "is_historic").formatNullable[Boolean]
      .inmap[Boolean](fromOptBool, toOptBool)
  )(Name.apply, unlift(Name.unapply))

}