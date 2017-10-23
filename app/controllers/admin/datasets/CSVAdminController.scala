package controllers.admin.datasets

import akka.actor.ActorSystem
import akka.stream.Materializer
import controllers.{ BaseAuthController, WebJarAssets }
import harvesting.loaders.StreamLoader
import javax.inject.{ Inject, Singleton }
import jp.t2v.lab.play2.auth.AuthElement
import org.joda.time.DateTime
import play.api.Configuration
import play.api.mvc.Action
import scala.concurrent.ExecutionContext
import services.item._
import services.item.importers.{ DatasetImporter, ItemImporter }
import services.user.{ Role, UserService }
import services.task.{ TaskService, TaskType }
import harvesting.crosswalks.CSVCrosswalk

@Singleton
class CSVAdminController @Inject() (
  val config: Configuration,
  val itemService: ItemService,
  val users: UserService,
  val taskService: TaskService,
  val materializer: Materializer,
  implicit val ctx: ExecutionContext,
  implicit val system: ActorSystem,
  implicit val webjars: WebJarAssets
) extends BaseAuthController with AuthElement {
  
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
      None,
      Seq.empty[String], // closeMatches
      Seq.empty[String]) // exactMatches
      
    importer.importRecord(record)
  }

  /** Bit of an ugly hack **/
  def index = StackAction(AuthorityKey -> Role.ADMIN) { implicit request =>
    Ok(views.html.admin.datasets.csv())
  }

  def importCSV = StackAction(AuthorityKey -> Role.ADMIN) { implicit request =>
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
            loggedIn.username)
        }
        
        Redirect(routes.CSVAdminController.index)
        
      case None => BadRequest
    }
  }

}
