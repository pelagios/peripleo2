package controllers.api

import controllers.{HasPrettyPrintJSON, HasVisitLogging}
import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.concurrent.{ExecutionContext, Future}
import services.item.ItemService
import services.item.reference.Reference
import services.notification._
import services.visit.VisitService

@Singleton
class ItemAPIController @Inject() (
  notificationService: NotificationService,
  implicit val itemService: ItemService,
  implicit val visitService: VisitService,
  implicit val ctx: ExecutionContext
) extends Controller with HasPrettyPrintJSON with HasVisitLogging {

  implicit val snippetWrites: Writes[(Reference, Seq[String])] = (
    (JsPath).write[Reference] and
    (JsPath \ "snippets").write[Seq[String]]
  )(t => (t._1, t._2))

  def getItem(identifier: String) = Action.async { implicit request =>
    itemService.findByIdentifier(identifier).map {
      case Some(item) => jsonOk(Json.toJson(item))
      case None => NotFound
    }
  }

  /** Lists the parts of this item **/
  def getParts(identifier: String, offset: Int, limit: Int) = Action.async { implicit request =>
    itemService.findByIdentifier(identifier).flatMap {
      case Some(item) => itemService.findByIsPartOf(item.isConflationOf.head, true, offset, limit).map(parts => jsonOk(Json.toJson(parts)))
      case None => Future.successful(NotFound)
    }
  }

  /** Lists the references contained in this item.
    *
    * Optionally, filtered by destination URL or query phrase (applied to the reference context)
    */
  def getReferences(identifier: String, to: Option[String], query: Option[String], offset: Int, limit: Int) = Action.async { implicit request =>
    itemService.getReferences(identifier, to, query).map { refs =>
      jsonOk(Json.toJson(refs))
    }
  }

  /** Lists information about top referenced items, along with reference statistics **/
  def getTopReferenced(identifier: String) = Action.async { implicit request =>
    itemService.getTopReferenced(identifier).map { stats =>
      jsonOk(Json.toJson(stats))
    }
  }
  
  /** Reports a client-side selection **/
  def reportSelection(identifier: String) = Action { implicit request =>
    logSelection(identifier)
    Ok
  }
  
  /** Reports a broken link associtated with a specific item **/
  def reportBrokenLink(identifier: String, brokenLink: String) = Action { implicit request => 
    val notification = 
      Notification(
        NotificationType.BROKEN_LINK,
        DateTime.now,
        Some(identifier),
        brokenLink)
        
    notificationService.insertNotification(notification)
    Ok
  }

}
