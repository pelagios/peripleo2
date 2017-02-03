package services.search

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

class Language private(val iso: String) {
  
  require(iso.size >= 2 && iso.size < 4) 
  
}

object Language {
  
  def apply(iso: String) = new Language(iso.toUpperCase)
  
  implicit val languageFormat: Format[Language] =
    Format(
      JsPath.read[JsString].map(json => Language(json.value)),
      Writes[Language](l => Json.toJson(l.iso))
    )
  
}