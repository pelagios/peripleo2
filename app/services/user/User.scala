package services.user

import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import services.HasDate


case class User(
  username     : String,
  email        : String,
  passwordHash : String,
  salt         : String,
  createdAt    : DateTime)
  
object User extends HasDate {
  
  implicit val userFormat: Format[User] = (
    (JsPath \ "username").format[String] and
    (JsPath \ "email").format[String] and
    (JsPath \ "password_hash").format[String] and
    (JsPath \ "salt").format[String] and
    (JsPath \ "created_at").format[DateTime]
  )(User.apply, unlift(User.unapply))
  
}