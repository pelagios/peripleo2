package controllers.api

import controllers.HasPrettyPrintJSON
import javax.inject.{ Inject, Singleton }
import play.api.libs.json.Json
import play.api.mvc.{ Action, Controller }
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.concurrent.{ ExecutionContext, Future }
import services.item.ItemService
import services.item.reference.Reference

@Singleton
class ItemAPIController @Inject() (
  itemService: ItemService,
  implicit val ctx: ExecutionContext
) extends Controller with HasPrettyPrintJSON {
  
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
    // The first .findByIdentifier isn't strictly needed (and introduces an ES roundtrip), but
    // IMO it makes sense to keep the API clean by providing proper HTTP 404 when the item doesn't exist
    // rather than just 0 results
    itemService.findByIdentifier(identifier).flatMap {
      case Some(item) => itemService.findPartsOf(identifier, offset, limit).map(parts => jsonOk(Json.toJson(parts)))          
      case None => Future.successful(NotFound)
    }
  }

  /** Lists information about related items, along with reference statistics **/
  def getRelated(identifier: String) = Action.async { implicit request =>
    itemService.getRelated(identifier).map { stats => 
      jsonOk(Json.toJson(stats))
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

}
