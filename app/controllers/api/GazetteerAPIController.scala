package controllers.api

import controllers.HasPrettyPrintJSON
import javax.inject.{ Inject, Singleton }
import play.api.mvc.{ Action, Controller }
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext
import services.item.search.{ SearchArgs, SearchService }

@Singleton
class GazetteerAPIController @Inject() (
  val searchService: SearchService,
  implicit val ctx: ExecutionContext
) extends Controller with HasPrettyPrintJSON {

  def list() = Action {

    // TODO implement
    
    Ok
  }

}
