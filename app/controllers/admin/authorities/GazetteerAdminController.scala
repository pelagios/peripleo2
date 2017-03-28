package controllers.admin.authorities

import akka.actor.ActorSystem
import akka.stream.Materializer
import controllers.{ BaseAuthController, WebJarAssets }
import harvesting.{ DumpImporter, StreamImporter }
import harvesting.crosswalks._
import java.io.FileInputStream
import java.util.UUID
import javax.inject.{ Inject, Singleton }
import jp.t2v.lab.play2.auth.AuthElement
import org.joda.time.DateTime
import play.api.{ Configuration, Logger }
import play.api.mvc.Action
import scala.concurrent.ExecutionContext
import services.task.TaskService
import services.user.{ Role, UserService }
import services.item._
import services.item.reference.UnboundReference
import services.item.place.PlaceService

@Singleton
class GazetteerAdminController @Inject() (
  val config: Configuration,
  val users: UserService,
  val itemService: ItemService,
  val placeService: PlaceService,
  val taskService: TaskService,
  val materializer: Materializer,
  implicit val ctx: ExecutionContext,
  implicit val system: ActorSystem,
  implicit val webjars: WebJarAssets
) extends BaseAuthController with AuthElement {

  private def upsertGazetteerMeta(filename: String) = {
    val name = filename.substring(0, filename.indexOf('.'))

    val gazetteer = ItemRecord(
      name,
      Seq(name),      
      DateTime.now,
      None, // lastChangedAt
      name,
      None, // isInDataset
      None, // isPartOf
      Seq.empty[Category],
      Seq.empty[Description],
      None, // homepage
      None, // license
      Seq.empty[Language],
      Seq.empty[Depiction],
      None, // geometry
      None, // representativePoint
      None, // temporalBounds
      Seq.empty[Name],
      Seq.empty[String], // closeMatches
      Seq.empty[String]) // exactMatches
      
    itemService.insertOrUpdateItem(Item.fromRecord(ItemType.DATASET.AUTHORITY.GAZETTEER, gazetteer), Seq.empty[UnboundReference])
  }

  def index = StackAction(AuthorityKey -> Role.ADMIN) { implicit request =>
    Ok(views.html.admin.authorities.gazetteers())
  }

  def importGazetteer = StackAction(AuthorityKey -> Role.ADMIN) { implicit request =>
    request.body.asMultipartFormData.flatMap(_.file("file")) match {
      case Some(formData) =>
        Logger.info("Importing gazetteer from " + formData.filename)


        /** TEMPORARY HACK **/

        upsertGazetteerMeta(formData.filename).map { success =>

          if (success) {

            if (formData.filename.contains(".ttl") || formData.filename.contains(".rdf")) {
              Logger.info("Importing Pelagios RDF/TTL dump")
              
              val importer = new DumpImporter(taskService)
              importer.importDump(
                formData.filename + " (Pelagios Gazetteer RDF)",
                formData.ref.file,
                formData.filename,
                PelagiosGazetteerCrosswalk.fromRDF(formData.filename),
                placeService,
                loggedIn.username)
                
            } else if (formData.filename.toLowerCase.contains("pleiades")) {
              Logger.info("Using Pleiades crosswalk")
              
              val importer = new StreamImporter(taskService, materializer)
              importer.importRecords(
                formData.filename + " (Pleiades GeoJSON)",
                formData.ref.file,
                formData.filename,
                PleiadesCrosswalk.fromJson,
                placeService,
                loggedIn.username)
                
            } else if (formData.filename.toLowerCase.contains("geonames")) {
              Logger.info("Using GeoNames crosswalk")
              
              val importer = new StreamImporter(taskService, materializer)
              importer.importRecords(
                formData.filename + " (GeoNames GeoJSON)",
                formData.ref.file,
                formData.filename,
                GeoNamesCrosswalk.fromJson,
                placeService,
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
