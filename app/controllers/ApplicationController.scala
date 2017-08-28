package controllers

import javax.inject.{ Inject, Singleton }
import play.api.mvc.{ Action, Controller }

@Singleton
class ApplicationController @Inject() (
  implicit val webjars: WebJarAssets
) extends Controller {

  def index() = Action { implicit request =>
    Ok(views.html.landing.index())
  }

  def ui() = Action { implicit request =>
    Ok(views.html.ui.index())
  }

}
