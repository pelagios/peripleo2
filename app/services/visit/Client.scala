package services.visit

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class Client(
  ip        : String,    
  userAgent : String,
  browser   : String,
  os        : String,
  deviceType: String
)
  
object Client {
  
  implicit val clientFormat: Format[Client] = (
    (JsPath \ "ip").format[String] and
    (JsPath \ "user_agent").format[String] and
    (JsPath \ "browser").format[String] and
    (JsPath \ "os").format[String] and
    (JsPath \ "device_type").format[String]
  )(Client.apply, unlift(Client.unapply))

}