package controllers.admin.authorities

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.mohiva.play.silhouette.api.Silhouette
import controllers.Security
import harvesting.loaders.{DumpLoader, StreamLoader}
import harvesting.crosswalks._
import javax.inject.{Inject, Singleton}
import org.webjars.play.WebJarsUtil
import play.api.{Configuration, Logger}
import play.api.mvc.ControllerComponents
import scala.concurrent.ExecutionContext
import services.task.{TaskService, TaskType}
import services.user.{Role, UserService}
import services.item.{ItemService, ItemType, PathHierarchy}
import services.item.importers.{DatasetImporter, EntityImporter}

@Singleton
class GazetteerAdminController @Inject() (
  val components: ControllerComponents,
  val config: Configuration,
  val users: UserService,
  val itemService: ItemService,
  val taskService: TaskService,
  val materializer: Materializer,
  val silhouette: Silhouette[Security.Env],
  implicit val ctx: ExecutionContext,
  implicit val system: ActorSystem,
  implicit val webjars: WebJarsUtil
) extends BaseAuthorityAdminController(components, new DatasetImporter(itemService, ItemType.DATASET.AUTHORITY.GAZETTEER)) {

  private val taskType = TaskType("GAZETTEER_IMPORT")

  def index = silhouette.SecuredAction(Security.WithRole(Role.ADMIN)) { implicit request =>
    Ok(views.html.admin.authorities.gazetteers())
  }

  def importGazetteer = silhouette.SecuredAction(Security.WithRole(Role.ADMIN)) { implicit request =>

    val importer = new EntityImporter(itemService, ItemType.PLACE)

    request.body.asMultipartFormData.flatMap(_.file("file")) match {

      case Some(formData) =>
        Logger.info("Importing gazetteer from " + formData.filename)

        /** TEMPORARY HACK **/
        val name = formData.filename.substring(0, formData.filename.indexOf('.'))
        upsertDatasetRecord(name, name).map { success =>
          if (success) {

            if (formData.filename.contains(".ttl") || formData.filename.contains(".rdf")) {
              Logger.info("Importing Pelagios RDF/TTL dump")

              new DumpLoader(taskService, taskType).importDump(
                formData.filename + " (Pelagios Gazetteer RDF)",
                formData.ref.path.toFile,
                formData.filename,
                PelagiosGazetteerCrosswalk.fromRDF(formData.filename),
                importer,
                request.identity.username)

            } else if (formData.filename.toLowerCase.contains("pleiades")) {
              Logger.info("Using Pleiades crosswalk")

              new StreamLoader(taskService, taskType, materializer).importRecords(
                formData.filename + " (Pleiades GeoJSON)",
                formData.ref.path.toFile,
                formData.filename,
                PleiadesCrosswalk.fromJson,
                importer,
                request.identity.username)
                
            } else if (formData.filename.toLowerCase.contains("idai")) {
              Logger.info("Using iDAI gazetteer crosswalk")
              
              new StreamLoader(taskService, taskType, materializer).importRecords(
                formData.filename + " (iDAI JSON)",
                formData.ref.path.toFile,
                formData.filename,
                IDAICrosswalk.fromJson(PathHierarchy(name, name)),
                importer,
                request.identity.username)

            } else if (formData.filename.toLowerCase.contains("geonames")) {
              Logger.info("Using GeoNames crosswalk")

              new StreamLoader(taskService, taskType, materializer).importRecords(
                formData.filename + " (GeoNames GeoJSON)",
                formData.ref.path.toFile,
                formData.filename,
                GeoNamesCrosswalk.fromJson(PathHierarchy(name, name)),
                importer,
                request.identity.username)
            } else if (formData.filename.toLowerCase.contains("europeana")) {
              Logger.info("Using Europeana simple places crosswalk")

              new StreamLoader(taskService, taskType, materializer).importRecords(
                formData.filename + " (Europeana GeoJSON)",
                formData.ref.path.toFile,
                formData.filename,
                EuropeanaPlacesCrosswalk.fromJson,
                importer,
                request.identity.username)
            } else if (formData.filename.endsWith("json")) {
              Logger.info("Using default GeoJSON crosswalk")

              new DumpLoader(taskService, taskType).importDump(
                formData.filename + " (GeoJSON Gazetteer)",
                formData.ref.path.toFile,
                formData.filename,
                FeatureCollectionCrosswalk.fromGeoJSON(PathHierarchy(name, name)),
                importer,
                request.identity.username)
            }

          }

        }

        /** TEMPORARY HACK **/

        Redirect(routes.GazetteerAdminController.index)

      case None => BadRequest

    }
  }

}
