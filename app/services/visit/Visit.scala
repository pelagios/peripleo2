package services.visit

import java.util.UUID
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import services.HasDate

/** TODO how do we extract "user intention"? Split up by query, filters, etc? **/
case class Visit(
    
  url: String,

  referer: Option[String],
   
  visitedAt: DateTime,
  
  client: Client
  
)

object Visit extends HasDate {
  
  implicit val visitFormat: Format[Visit] = (
    (JsPath \ "url").format[String] and
    (JsPath \ "referer").formatNullable[String] and
    (JsPath \ "visited_at").format[DateTime] and
    (JsPath \ "client").format[Client]
  )(Visit.apply, unlift(Visit.unapply))
  
}

case class Client(
  
  ip: String,
  
  userAgent: String,
  
  browser: String,
  
  os: String,
  
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