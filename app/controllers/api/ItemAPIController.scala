package controllers.api

import controllers.HasPrettyPrintJSON
import javax.inject.{ Inject, Singleton }
import play.api.mvc.{ Action, Controller }
import scala.concurrent.ExecutionContext
import services.item.ItemService
import play.api.libs.json.Json

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

  def getParts(identifier: String) = Action.async { implicit request =>
    itemService.findPartsOf(identifier).map { parts => 
      jsonOk(Json.toJson(parts))
    }
  }

}
