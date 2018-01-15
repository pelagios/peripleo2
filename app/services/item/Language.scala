package services.item

import play.api.Logger
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import scala.util.{Try, Success, Failure}

class Language private(val iso: String) {

  require(iso.size >= 2 && iso.size < 4, iso)

  override def equals(that: Any): Boolean =
    that match {
      case that: Language => iso.equals(that.iso)
      case _ => false
    }

  override def hashCode = iso.hashCode

  override def toString = iso

}

object Language {

  // That's the reason we can't have Language as a case class - would
  // lead to conflicting method signatures
  def apply(iso: String) = new Language(iso.toUpperCase)

  /** Alternative builder that returns Option instead of failing on invalid input **/
  def safeParse(str: String): Option[Language] =
    Try(Language(str)) match {
      case Success(language) =>
        Some(language)
        
      case Failure(t) =>
        Logger.warn("Error parsing language code: " + str)
        None
    }

  implicit val languageFormat: Format[Language] =
    Format(
      JsPath.read[JsString].map(json => Language(json.value)),
      Writes[Language](l => Json.toJson(l.iso))
    )

}
