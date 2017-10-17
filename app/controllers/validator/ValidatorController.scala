package controllers.validator

import controllers.WebJarAssets
import javax.inject.{ Inject, Singleton }
import play.api.mvc.{ Action, Controller }

@Singleton
class ValidatorController @Inject() (
  implicit val webjars: WebJarAssets
) extends Controller {

  def index = Action { implicit request =>
    Ok(views.html.validator.index())
  }

  def gazetteer = Action { implicit request =>
    Ok(views.html.validator.gazetteer())
  }

  def validateGazetteer = Action.async { implicit request =>
    null // Ok
  }

  def annotations = Action { implicit request =>
    Ok(views.html.validator.annotations())
  }

  def validateAnnotations = Action.async { implicit request =>
    null // Ok
  }

}
