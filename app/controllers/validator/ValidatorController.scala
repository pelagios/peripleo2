package controllers.validator

import controllers.WebJarAssets
import javax.inject.{ Inject, Singleton }
import play.api.mvc.{ Action, Controller }
import scala.concurrent.Future

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

  def validateGazetteer = Action.async(parse.multipartFormData) { implicit request =>
    request.body.file("file").map { filepart =>          
      request.body.dataParts.get("format").map(_.head) match {
        case Some(format) =>
          val file = filepart.ref.file
          if (file.length > 0) {

            play.api.Logger.info("validating " + filepart.filename + " as " + format)
            
            Future.successful(Ok)
          } else {
            Future.successful(Redirect(routes.ValidatorController.gazetteer).flashing("error" -> "Please attach a data file"))
          }

        case None =>
          Future.successful(Redirect(routes.ValidatorController.gazetteer).flashing("error" -> "Please select a data format"))
      }      
    }.getOrElse {
      // Can never happen, unless someone submits an off-form/hacked request
      Future.successful(BadRequest)
    }    
  }

  def annotations = Action { implicit request =>
    Ok(views.html.validator.annotations())
  }

  def validateAnnotations = Action.async { implicit request =>
    null // Ok
  }

}
