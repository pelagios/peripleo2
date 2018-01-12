package controllers.admin.datasets

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseAuthController, Security}
import harvesting.loaders.StreamLoader
import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.mvc.ControllerComponents
import scala.concurrent.ExecutionContext
import services.item._
import services.item.importers.{DatasetImporter, ItemImporter}
import services.user.{Role, UserService}
import services.task.{TaskService, TaskType}
import harvesting.crosswalks.CSVCrosswalk

@Singleton
class CSVAdminController @Inject() (
  val components: ControllerComponents,
  val config: Configuration,
  val itemService: ItemService,
  val users: UserService,
  val taskService: TaskService,
  val materializer: Materializer,
  val silhouette: Silhouette[Security.Env],
  implicit val ctx: ExecutionContext,
  implicit val system: ActorSystem,
  implicit val webjars: WebJarsUtil
) extends BaseAuthController(components){

  private def upsertDatasetRecord(title: String) = {
    val importer = new DatasetImporter(itemService, ItemType.DATASET.ANNOTATIONS)
    val record = ItemRecord(
      title,
      Seq(title),
      DateTime.now,
      None, // lastChangedAt
      title,
      None, None, // isInDataset, isPartOf
      Seq.empty[Category],
      Seq.empty[Description],
      None, // homepage
      None,
      Seq.empty[Language],
      Seq.empty[Depiction],
      None, None, None, // geometry, representativePoint, temporalBounds
      Seq.empty[Name],
      Seq.empty[Link],
      None, None)

    importer.importRecord(record)
  }

  /** Bit of an ugly hack **/
  def index = silhouette.SecuredAction(Security.WithRole(Role.ADMIN)) { implicit request =>
    Ok(views.html.admin.datasets.csv())
  }

  def importCSV = silhouette.SecuredAction(Security.WithRole(Role.ADMIN)) { implicit request =>
    request.body.asMultipartFormData.flatMap(_.file("file")) match {
      case Some(formdata) =>
        val importer = new ItemImporter(itemService, ItemType.OBJECT)

        // TODO temporary hack - should have proper URI + title
        val name = formdata.filename.substring(0, formdata.filename.indexOf('.'))
        val dataset = PathHierarchy(name, name)

        upsertDatasetRecord(name).map { success =>
          new StreamLoader(taskService, TaskType("CSV_IMPORT"), materializer).importRecords(
            formdata.filename,
            formdata.ref.file,
            formdata.filename,
            CSVCrosswalk.fromCSV(formdata.ref.file, dataset),
            importer,
            request.identity.username)
        }

        Redirect(routes.CSVAdminController.index)

      case None => BadRequest
    }
  }

}
