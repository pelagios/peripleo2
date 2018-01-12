package controllers.validator

import harvesting.crosswalks.{FeatureCollectionCrosswalk, PelagiosAnnotationCrosswalk, PelagiosGazetteerCrosswalk}
import java.io.{File, FileInputStream}
import javax.inject.{Inject, Singleton}
import java.util.zip.GZIPInputStream
import org.webjars.play.WebJarsUtil
import play.api.mvc.{AbstractController, ControllerComponents}
import scala.concurrent.{ ExecutionContext, Future }
import services.item.PathHierarchy

@Singleton
class ValidatorController @Inject() (
  val components: ControllerComponents,
  implicit val ctx: ExecutionContext,
  implicit val webjars: WebJarsUtil
) extends AbstractController(components) {

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
          val file = filepart.ref.path.toFile
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
    }.recover { case t: Throwable =>
      Redirect(routes.ValidatorController.gazetteer).flashing("error" -> t.getMessage)
    }
  }

  def annotations = Action { implicit request =>
    Ok(views.html.validator.annotations())
  }

  def validateAnnotations = Action.async(parse.multipartFormData) { implicit request =>
    request.body.file("file").map { filepart =>
      val file = filepart.ref.path.toFile
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
