package controllers.api

import controllers.{ HasPrettyPrintJSON, WebJarAssets }
import javax.inject.{ Inject, Singleton }
import play.api.mvc.{ Action, Controller }
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext
import services.item.search.{ SearchArgs, SearchService }

@Singleton
class SearchAPIController @Inject() (
  val searchService: SearchService,
  implicit val ctx: ExecutionContext,
  implicit val webjars: WebJarAssets
) extends Controller with HasPrettyPrintJSON {
  
  def search() = Action.async { implicit request =>
    val args = SearchArgs.fromQueryString(request.queryString)
    searchService.query(args).map(results => jsonOk(Json.toJson(results)))
  }
  
}
