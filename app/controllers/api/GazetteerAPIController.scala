package controllers.api

import controllers.HasPrettyPrintJSON
import javax.inject.{ Inject, Singleton }
import play.api.libs.json.Json
import play.api.mvc.{ Action, Controller }
import scala.concurrent.ExecutionContext
import services.item.search.{ SearchArgs, SearchService, SearchFilters, ResponseSettings }
import services.item.search.filters.TermFilter

@Singleton
class GazetteerAPIController @Inject() (
  searchService: SearchService,
  implicit val ctx: ExecutionContext
) extends Controller with HasPrettyPrintJSON {

  def list() = Action.async { implicit request =>

    // Filter item_type = PLACE
    val filters = SearchFilters(
      Some(TermFilter(Seq("PLACE"), TermFilter.ONLY)), 
      None, None, None, None, None, None, None)

    // Keep only term aggregations
    val settings = ResponseSettings(false, true, false)
    
    val searchArgs = SearchArgs(
      None, // query
      0, 0, // offset, limit
      filters,
      settings)
    
    searchService.query(searchArgs).map { results =>
      
      // TODO convert response. we don't want a Page, but instead just the gazetter stats
      
      jsonOk(Json.toJson(results))
    }
  }

}
