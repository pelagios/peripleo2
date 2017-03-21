package controllers.api

import controllers.HasPrettyPrintJSON
import javax.inject.{ Inject, Singleton }
import play.api.mvc.{ Action, Controller }

@Singleton
class ItemAPIController @Inject() () extends Controller with HasPrettyPrintJSON {
  
  def getItem(identifier: String) = Action { implicit request =>
    // TODO implement
    Ok
  }
  
  def getReferences(identifier: String) = Action.async { implicit request =>
    null
  }
  
}