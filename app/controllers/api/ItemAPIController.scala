package controllers.api

import controllers.HasPrettyPrintJSON
import javax.inject.{ Inject, Singleton }
import play.api.libs.json.Json
import play.api.mvc.{ Action, Controller }
import scala.concurrent.{ ExecutionContext, Future }
import services.item.ItemService

@Singleton
class ItemAPIController @Inject() (
  itemService: ItemService,
  implicit val ctx: ExecutionContext
) extends Controller with HasPrettyPrintJSON {

  def getItem(identifier: String) = Action.async { implicit request =>
    itemService.findByIdentifier(identifier).map {
      case Some(item) => jsonOk(Json.toJson(item))
      case None => NotFound
    }
  }

  def getReferences(identifier: String) = Action.async { implicit request =>
    itemService.getReferenceStats(identifier).map { stats =>
      jsonOk(Json.toJson(stats))
    }
  }

  def getParts(identifier: String, offset: Int, limit: Int) = Action.async { implicit request =>
    // The first .findByIdentifier isn't strictly needed (and introduces an ES roundtrip), but
    // IMO it makes sense to keep the API clean by providing proper HTTP 404 when the item doesn't exist
    // rather than just 0 results
    itemService.findByIdentifier(identifier).flatMap {
      case Some(item) => itemService.findPartsOf(identifier, offset, limit).map(parts => jsonOk(Json.toJson(parts)))          
      case None => Future.successful(NotFound)
    }
  }

}
