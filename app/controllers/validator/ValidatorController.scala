package controllers.validator

import controllers.WebJarAssets
import javax.inject.{ Inject, Singleton }
import play.api.mvc.{ Action, Controller }
import scala.concurrent.Future
import harvesting.crosswalks.FeatureCollectionCrosswalk
import java.io.FileInputStream
import scala.concurrent.{ ExecutionContext, Future }
import services.item.PathHierarchy
import harvesting.crosswalks.PelagiosGazetteerCrosswalk
import java.util.zip.GZIPInputStream
import java.io.File

@Singleton
class ValidatorController @Inject() (
  implicit val ctx: ExecutionContext,
  implicit val webjars: WebJarAssets
) extends Controller {

  def index = Action { implicit request =>
    Ok(views.html.validator.index())
  }

  def gazetteer = Action { implicit request =>
    Ok(views.html.validator.gazetteer())
  }

  def validateGazetteer = Action.async(parse.multipartFormData) { implicit request =>
    
    def getCrosswalk(format: String, filename: String) = {
      val p = PathHierarchy("http://www.example.com", "Example Dataset")
      
      format match {
        case "geojson" => Some(FeatureCollectionCrosswalk.fromGeoJSON(p))
        case "rdf" => Some(PelagiosGazetteerCrosswalk.fromRDF(filename))
        case _ => None
      }
    }
    
    def getInputStream(filename: String, file: File) = 
      if (filename.endsWith(".gz")) new GZIPInputStream(new FileInputStream(file))
      else new FileInputStream(file)
    
    request.body.file("file").map { filepart =>  
      request.body.dataParts.get("format").map(_.head) match {
        case Some(format) =>
          val file = filepart.ref.file
          if (file.length > 0) {
            getCrosswalk(format, filepart.filename) match {
              case Some(crosswalk) => Future {
                val records = crosswalk(getInputStream(filepart.filename, file))
                if (records.isEmpty) {
                  Redirect(routes.ValidatorController.gazetteer).flashing("warning" -> "File parsed, but did not contain any places.")
                } else {
                  Redirect(routes.ValidatorController.gazetteer).flashing("success" -> s"Success! Found ${records.size} places.")
                }
              }
                
              case None =>
                // No crosswalk? Can never happen, unless someone submits an off-form/hacked request
                Future.successful(BadRequest)
            }
          } else {
            Future.successful(Redirect(routes.ValidatorController.gazetteer).flashing("error" -> "Please attach a data file."))
          }

        case None =>
          Future.successful(Redirect(routes.ValidatorController.gazetteer).flashing("error" -> "Please select a data format."))
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
