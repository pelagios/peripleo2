package services.notification

import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import services.HasDate

case class Notification(
  notificationType : NotificationType.Value,
  at               : DateTime,
  concerns         : Option[String],
  message          : String)

object Notification extends HasDate {
  
  implicit val notificationFormat: Format[Notification] = (
    (JsPath \ "notification_type").format[NotificationType.Value] and
    (JsPath \ "at").format[DateTime] and
    (JsPath \ "concerns").formatNullable[String] and
    (JsPath \ "message").format[String]
  )(Notification.apply, unlift(Notification.unapply))
  
}
