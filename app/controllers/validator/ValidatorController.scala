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
import harvesting.crosswalks.PelagiosAnnotationCrosswalk

@Singleton
class ValidatorController @Inject() (
  implicit val ctx: ExecutionContext,
  implicit val webjars: WebJarAssets
) extends Controller {
  
  private val DUMMY_DATASET = PathHierarchy("http://www.example.com", "Example Dataset")

  private def getInputStream(filename: String, file: File) = 
    if (filename.endsWith(".gz")) new GZIPInputStream(new FileInputStream(file))
    else new FileInputStream(file)
  
  def index = Action { implicit request =>
    Ok(views.html.validator.index())
  }

  def gazetteer = Action { implicit request =>
    Ok(views.html.validator.gazetteer())
  }

  def validateGazetteer = Action.async(parse.multipartFormData) { implicit request =>
    
    def getCrosswalk(format: String, filename: String) =
      format match {
        case "geojson" => Some(FeatureCollectionCrosswalk.fromGeoJSON(DUMMY_DATASET))
        case "rdf" => Some(PelagiosGazetteerCrosswalk.fromRDF(filename))
        case _ => None
      }
    
    request.body.file("file").map { filepart =>  
      request.body.dataParts.get("format").map(_.head) match {
        case Some(format) =>
          val file = filepart.ref.file
          if (file.length > 0) {
            getCrosswalk(format, filepart.filename) match {
              case Some(crosswalk) => Future {
                val records = crosswalk(getInputStream(filepart.filename, file))
                if (records.isEmpty)
                  Redirect(routes.ValidatorController.gazetteer).flashing("warning" -> "File parsed, but did not contain any places.")
                else
                  Redirect(routes.ValidatorController.gazetteer).flashing("success" -> s"Success! Found ${records.size} places.")
              }
                
              case None =>
                // No crosswalk? Can never happen, unless someone submits an off-form/hacked request
                Future.successful(BadRequest)
            }
          } else {
            Future.successful(Redirect(routes.ValidatorController.gazetteer).flashing("error" -> "Please attach a data file!"))
          }

        case None =>
          Future.successful(Redirect(routes.ValidatorController.gazetteer).flashing("error" -> "Please select a data format!"))
      }      
    }.getOrElse {
      // No filepart? Can never happen, unless someone submits an off-form/hacked request
      Future.successful(BadRequest)
    }    
  }

  def annotations = Action { implicit request =>
    Ok(views.html.validator.annotations())
  }

  def validateAnnotations = Action.async(parse.multipartFormData) { implicit request =>
    request.body.file("file").map { filepart =>
      val file = filepart.ref.file
      if (file.length > 0) {
        val crosswalk = PelagiosAnnotationCrosswalk.fromRDF(filepart.filename, DUMMY_DATASET)
        
        // Long-running operation
        Future {
          val records = crosswalk(getInputStream(filepart.filename, file))
          if (records.isEmpty)
            Redirect(routes.ValidatorController.annotations).flashing("warning" -> "File parsed, but did not contain any items.")
          else
            Redirect(routes.ValidatorController.annotations).flashing("success" -> s"Success! Found ${records.size} items.")
        }
        
      } else {
        Future.successful(Redirect(routes.ValidatorController.annotations).flashing("error" -> "Please attach a data file!"))
      }
    }.getOrElse {
      // No filepart? Can never happen, unless someone submits an off-form/hacked request
      Future.successful(BadRequest)
    } 
  }

}
