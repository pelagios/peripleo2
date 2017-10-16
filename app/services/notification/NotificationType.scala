package services.notification

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

object NotificationType extends Enumeration {

  val SYSTEM_ERROR = Value("SYSTEM_ERROR")
  
  val BROKEN_LINK = Value("BROKEN_LINK")
  
  implicit val notificationTypeFormat: Format[NotificationType.Value] =
    Format(
      JsPath.read[JsString].map(json => NotificationType.withName(json.value)),
      Writes[NotificationType.Value](s => Json.toJson(s.toString))
    )

}