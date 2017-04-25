package services.item.search

import play.api.Logger
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import services.item.Item
import services.notification._
import org.joda.time.DateTime

case class TopPlaces private(places: Seq[(Item, Long)])

object TopPlaces {
  
  def build(counts: Seq[(String, Long)], places: Seq[Item])(implicit notifications: NotificationService): TopPlaces = {
    
    def logError(triedIds: Seq[String], resolvedIds: Seq[String]) = {
      val failedIds = triedIds diff resolvedIds
      Logger.error("Error resolving places")
      Logger.error("  failed: " + failedIds.mkString(", "))
      Logger.error(failedIds.size + " out of " + triedIds.size)
      
      notifications.insertNotification(Notification(
        NotificationType.SYSTEM_ERROR, DateTime.now,
        "Failed to resolve " + failedIds.mkString(", ") + " (" + failedIds.size + " out of " + triedIds.size + ")"))
    }
    
    val resolved = counts.flatMap { case (docId, count) =>
      val maybeResolved = places.find(_.docId.toString == docId)
      maybeResolved.map(place => (place, count))
    }
    
    if (resolved.size < counts.size)
      logError(counts.map(_._1), resolved.map(_._1.docId.toString))
    
    TopPlaces(resolved)
  }
    
  implicit val topPlacesWrites = 
    Writes[TopPlaces](p => Json.toJson(p.places.map { case (place, count) => 
      Json.toJson(place).as[JsObject] ++ Json.obj("result_count" -> count) }))
    
}