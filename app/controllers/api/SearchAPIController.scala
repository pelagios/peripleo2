package controllers.api

import controllers.{ HasPrettyPrintJSON, HasVisitLogging }
import javax.inject.{ Inject, Singleton }
import play.api.mvc.{ Action, Controller }
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext
import services.item.search.{ SearchArgs, SearchService, SuggestService }
import services.visit.VisitService

@Singleton
class SearchAPIController @Inject() (
  val searchService: SearchService,
  val suggestService: SuggestService,
  implicit val visitService: VisitService,
  implicit val ctx: ExecutionContext
) extends Controller with HasPrettyPrintJSON with HasVisitLogging {
  
  def search() = Action.async { implicit request =>
    logRequest()
    
    val args = SearchArgs.fromQueryString(request.queryString)
    searchService.query(args).map(results => jsonOk(Json.toJson(results)))
  }
  
  def suggest(q: String) = Action.async { implicit request =>
    suggestService.suggest(q).map(options => jsonOk(Json.toJson(options)))
  }
  
}
