package services.user

import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import services.HasDate

object Role {

  sealed trait Role 

  case object ADMIN extends Role
  case object PARTNER extends Role
  
  implicit val roleFormat: Format[Role] =
    Format(
      JsPath.read[JsString].map { _.toString match {
        case "ADMIN" => ADMIN
        case "PARTNER" => PARTNER
      }},
      
      Writes[Role] { role => Json.toJson(role.toString) }
    )
  
}

case class AccessLevel(role: Role.Role, affiliation: Option[String] = None)

object AccessLevel {
 
  implicit val accesslevelFormat: Format[AccessLevel] = (
    (JsPath \ "role").format[Role.Role] and
    (JsPath \ "affiliation").formatNullable[String]
  )(AccessLevel.apply, unlift(AccessLevel.unapply))
  
}

case class User(
  username     : String,
  email        : String,
  passwordHash : String,
  salt         : String,
  accessLevel  : AccessLevel,
  createdAt    : DateTime)
  
object User extends HasDate {
  
  implicit val userFormat: Format[User] = (
    (JsPath \ "username").format[String] and
    (JsPath \ "email").format[String] and
    (JsPath \ "password_hash").format[String] and
    (JsPath \ "salt").format[String] and
    (JsPath \ "access_level").format[AccessLevel] and
    (JsPath \ "created_at").format[DateTime]
  )(User.apply, unlift(User.unapply))
    
}