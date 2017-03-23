package controllers.api

import controllers.HasPrettyPrintJSON
import javax.inject.{ Inject, Singleton }
import play.api.mvc.{ Action, Controller }
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext
import services.item.search.{ SearchArgs, SearchService, SuggestService }

@Singleton
class SearchAPIController @Inject() (
  val searchService: SearchService,
  val suggestService: SuggestService,
  implicit val ctx: ExecutionContext
) extends Controller with HasPrettyPrintJSON {
  
  def search() = Action.async { implicit request =>
    val args = SearchArgs.fromQueryString(request.queryString)
    searchService.query(args).map(results => jsonOk(Json.toJson(results)))
  }
  
  def suggest(q: String) = Action.async { implicit request =>
    suggestService.suggest(q).map(options => jsonOk(Json.toJson(options)))
  }
  
}
