package controllers.api

import controllers.HasPrettyPrintJSON
import javax.inject.{ Inject, Singleton }
import play.api.mvc.{ Action, Controller }
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext
import services.item.search.{ ResponseSettings, SearchArgs, SearchFilters, SearchService }
import services.item.search.filters.TermFilter

@Singleton
class DatasetAPIController @Inject() (
  searchService: SearchService,
  implicit val ctx: ExecutionContext
) extends Controller with HasPrettyPrintJSON {
  
  def list(offset: Int, limit: Int, rootOnly: Boolean) = Action.async { implicit request =>
    // Filter item_type = DATASET
    val filters = SearchFilters(
      Some(TermFilter(Seq("DATASET"), TermFilter.ONLY)), 
      None, None, None, None, None, None, None, true)
      
    val settings = ResponseSettings(false, false, false)
    
    val searchArgs = SearchArgs(
      None, // query
      limit,
      offset,
      filters,
      settings)
      
    searchService.query(searchArgs).map { results =>
      
      // TODO convert response. we don't want a Page, but instead just the gazetter stats
      
      jsonOk(Json.toJson(results))
    }
  }
  
}