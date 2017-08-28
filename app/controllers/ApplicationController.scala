package controllers

import javax.inject.{ Inject, Singleton }
import play.api.mvc.{ Action, Controller }

@Singleton
class ApplicationController @Inject() (
  implicit val webjars: WebJarAssets
) extends Controller {

  def index() = Action { implicit request =>
    Ok(views.html.index())
  }

  def map() = Action { implicit request =>
    Ok(views.html.map())
  }

}
