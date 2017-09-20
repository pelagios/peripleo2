package controllers.api

import controllers.{ HasPrettyPrintJSON, HasDatasetStats }
import javax.inject.{ Inject, Singleton }
import play.api.mvc.{ Action, Controller }
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.concurrent.ExecutionContext
import services.item.{ Item, ItemService, ItemType }
import services.item.search.{ ResponseSettings, SearchArgs, SearchFilters, SearchService }
import services.item.search.filters.TermFilter

@Singleton
class DatasetAPIController @Inject() (
  itemService: ItemService,
  implicit val searchService: SearchService,
  implicit val ctx: ExecutionContext
) extends Controller with HasPrettyPrintJSON with HasDatasetStats {
  
  def list(offset: Int, limit: Int, rootOnly: Boolean) = Action.async { implicit request =>
    
    val fListDatasets = itemService.findByType(ItemType.DATASET, true, offset, limit)
    
    val f = for {
      datasets <- fListDatasets
      withStats <- addStats(datasets)  
    } yield (withStats)
 
    f.map { result =>
      
      implicit val resultWrites: Writes[(Item, Long)] = (
        (JsPath).write[Item] and
        (JsPath \ "items").write[Long]
      )(t => (t._1, t._2)) 
      
      jsonOk(Json.toJson(result))   
    }    
  }
  
}