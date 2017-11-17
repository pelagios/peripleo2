package controllers.api.legacy

import controllers.api.legacy.response._
import controllers.{HasPrettyPrintJSON, HasVisitLogging}
import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, Controller, Request}
import play.api.libs.json.{Json, JsValue}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import services.Page
import services.item.{ItemService, ItemType}
import services.item.search.SearchService
import services.visit.VisitService

@Singleton
class LegacyAPIController @Inject() (
  implicit val itemService: ItemService,
  implicit val searchService: SearchService,
  implicit val visitService: VisitService,
  implicit val ctx: ExecutionContext
) extends Controller with HasVisitLogging {
  
  private def jsonOk(obj: JsValue)(implicit request: Request[AnyContent]) = {
    val pretty = Try(request.queryString.get("prettyprint").map(_.head.toBoolean).getOrElse(false)).getOrElse(false)
    if (pretty) Ok(Json.prettyPrint(obj)).as("application/json") else Ok(obj)
  }
    
  def search() = Action.async { implicit request =>
    val args = LegacySearchArgs.fromQueryString(request.queryString)
    searchService.query(args).map { r => 
      logSearch(args, r)      
      val legacyFormat = 
        Page(r.took, r.total, r.offset, r.limit, r.items.map(LegacySearchResult.fromItem(_)))
      jsonOk(Json.toJson(legacyFormat))
    }
  }
  
  def getItem(id: String) = Action.async { implicit request =>
    itemService.findByIdentifier(id).flatMap {
      case Some(item) =>
        val fReferenceCount = itemService.countReferences(id)
        val fPlaceCount = itemService.countReferencesToType(id, ItemType.PLACE)
        val f = for {
          refCount <- fReferenceCount
          placeCount <- fPlaceCount
        } yield (refCount._1, placeCount._2)
        
        f.map { case (references, places) =>
          jsonOk(Json.toJson(LegacyItem.fromItem(item, references, places)))
        }
            
      case None =>
        Future.successful(NotFound)
    }
  }
  
  def getPlace(id: String) = Action.async { implicit request =>
    itemService.findByIdentifier(id).flatMap {
      case Some(item) =>
        // TODO
        Future.successful(Ok)
        
      case None =>
        Future.successful(NotFound)
    }
  }
  
  

}