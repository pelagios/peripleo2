package controllers.admin.authorities

import akka.actor.ActorSystem
import akka.stream.Materializer
import controllers.{ BaseAuthController, WebJarAssets }
import harvesting.loaders.{ DumpLoader, StreamLoader }
import harvesting.crosswalks._
import java.io.FileInputStream
import java.util.UUID
import javax.inject.{ Inject, Singleton }
import jp.t2v.lab.play2.auth.AuthElement
import org.joda.time.DateTime
import play.api.{ Configuration, Logger }
import play.api.mvc.Action
import scala.concurrent.{ Future, ExecutionContext }
import services.task.TaskService
import services.user.{ Role, UserService }
import services.item._
import services.item.reference.UnboundReference
import services.item.importers.{ DatasetImporter, EntityImporter }

@Singleton
class GazetteerAdminController @Inject() (
  val config: Configuration,
  val users: UserService,
  val itemService: ItemService,
  val taskService: TaskService,
  val materializer: Materializer,
  implicit val ctx: ExecutionContext,
  implicit val system: ActorSystem,
  implicit val webjars: WebJarAssets
) extends BaseAuthorityAdminController(new DatasetImporter(itemService)) {

  private def upsertGazetteerMeta(filename: String) = {    
    val name = filename.substring(0, filename.indexOf('.'))
    upsertDatasetRecord(
      ItemType.DATASET.AUTHORITY.GAZETTEER,
      name, // TODO should be a URI
      name)
  }

  def index = StackAction(AuthorityKey -> Role.ADMIN) { implicit request =>
    Ok(views.html.admin.authorities.gazetteers())
  }

  def importGazetteer = StackAction(AuthorityKey -> Role.ADMIN) { implicit request =>
    
    val importer = new EntityImporter(itemService, ItemType.PLACE)
    
    request.body.asMultipartFormData.flatMap(_.file("file")) match {
      case Some(formData) =>
        Logger.info("Importing gazetteer from " + formData.filename)


        /** TEMPORARY HACK **/

        upsertGazetteerMeta(formData.filename).map { success =>

          if (success) {

            if (formData.filename.contains(".ttl") || formData.filename.contains(".rdf")) {
              Logger.info("Importing Pelagios RDF/TTL dump")
              
              new DumpLoader(taskService).importDump(
                formData.filename + " (Pelagios Gazetteer RDF)",
                formData.ref.file,
                formData.filename,
                PelagiosGazetteerCrosswalk.fromRDF(formData.filename),
                importer,
                loggedIn.username)
                
            } else if (formData.filename.toLowerCase.contains("pleiades")) {
              Logger.info("Using Pleiades crosswalk")
              
              new StreamLoader(taskService, materializer).importRecords(
                formData.filename + " (Pleiades GeoJSON)",
                formData.ref.file,
                formData.filename,
                PleiadesCrosswalk.fromJson,
                importer,
                loggedIn.username)
                
            } else if (formData.filename.toLowerCase.contains("geonames")) {
              Logger.info("Using GeoNames crosswalk")
              
              new StreamLoader(taskService, materializer).importRecords(
                formData.filename + " (GeoNames GeoJSON)",
                formData.ref.file,
                formData.filename,
                GeoNamesCrosswalk.fromJson,
                importer,
                loggedIn.username)
            }

          }

        }

        /** TEMPORARY HACK **/

        Redirect(routes.GazetteerAdminController.index)

      case None => BadRequest

    }
  }

}
