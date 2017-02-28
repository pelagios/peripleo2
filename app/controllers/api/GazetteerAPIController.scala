package controllers.api

import controllers.HasPrettyPrintJSON
import javax.inject.{ Inject, Singleton }
import play.api.libs.json.Json
import play.api.mvc.{ Action, Controller }
import scala.concurrent.ExecutionContext
import services.item.search.{ SearchArgs, SearchService }

@Singleton
class GazetteerAPIController @Inject() (
  searchService: SearchService,
  implicit val ctx: ExecutionContext
) extends Controller with HasPrettyPrintJSON {

  def list() = Action.async { implicit request =>
    
    // TODO filter by item_type = PLACE
    
    // TODO timehistogram -> off
    
    val searchArgs = SearchArgs(
      None, // query
      0, 0, // offset, limit
      null,
      null
    )
    
    searchService.query(searchArgs).map { results =>
      
      // TODO convert response. we don't want a Page, but instead just the gazetter stats
      
      jsonOk(Json.toJson(results))
    }
  }

}
