package controllers.admin.datasets

import akka.actor.ActorSystem
import controllers.WebJarAssets
import controllers.admin.authorities.BaseAuthorityAdminController
import harvesting.VoIDHarvester
import harvesting.loaders.DumpLoader
import harvesting.crosswalks._
import javax.inject.{ Inject, Singleton }
import play.api.{ Configuration, Logger }
import play.api.mvc.MultipartFormData
import play.api.libs.Files
import scala.concurrent.ExecutionContext
import services.item.{ ItemType, PathHierarchy } 
import services.item.importers.DatasetImporter
import services.task.{ TaskService, TaskType }
import services.user.{ Role, UserService }
import services.item.ItemService
import services.item.importers.ItemImporter
import harvesting.crosswalks.tei.TeiCrosswalk
import org.pelagios.api.dataset.Dataset

@Singleton
class AnnotationsAdminController @Inject() (
  val config: Configuration,
  val itemService: ItemService,
  val taskService: TaskService,
  val users: UserService,
  val voidHarvester: VoIDHarvester,
  implicit val ctx: ExecutionContext,
  implicit val system: ActorSystem,
  implicit val webjars: WebJarAssets
) extends BaseAuthorityAdminController(new DatasetImporter(itemService, ItemType.DATASET.ANNOTATIONS)) {
  
  def index = AsyncStack(AuthorityKey -> Role.ADMIN) { implicit request =>
    itemService.findByType(ItemType.DATASET.ANNOTATIONS).map { page =>
      Ok(views.html.admin.datasets.annotations(page))
    }
  }
  
  def importData = StackAction(AuthorityKey -> Role.ADMIN) { implicit request =>
    
    val voidReq = request.body.asFormUrlEncoded
    val fileReq = request.body.asMultipartFormData
    
    def harvestVoID(form: Map[String, Seq[String]]) = form.get("url").flatMap(_.headOption) match {
      case Some(url) =>
        voidHarvester.harvest(url, loggedIn.username).map { success =>
          if (success) Logger.info(s"Import complete from $url")
          else Logger.error(s"Import failed from $url")
        } recover { case t: Throwable =>
          t.printStackTrace
          Logger.info("Error harvesting VoID: " + t.getMessage)
        }
        Ok // Return immediately
          
      case _ => BadRequest
    }
    
    def importFile(form: MultipartFormData[Files.TemporaryFile]) = form.file("file") match {
      case Some(filepart) =>
        if (filepart.filename.contains(".tei.xml")) {
          Logger.info("Importing TEI")
          
          // TODO just a hack for now!!
          upsertDatasetRecord("TEI", "TEI").map { success => if (success) {
            val importer = new ItemImporter(itemService, ItemType.OBJECT)
            new DumpLoader(taskService, TaskType("TEI_IMPORT")).importDump(
              filepart.filename,
              filepart.ref.file,
              filepart.filename,
              TeiCrosswalk.fromSingleFile(filepart.filename, PathHierarchy("TEI", "TEI")),
              importer,
              loggedIn.username)
          }}
          // TODO just a hack for now!!
          
        }
        
        Redirect(routes.AnnotationsAdminController.index)
        
      case None => BadRequest
    }
    
    (voidReq, fileReq) match {
      case (Some(form), _) => harvestVoID(form)
      case (_, Some(form)) => importFile(form)
      case _ => BadRequest
    }
    
  }
  
  def deleteDataset(id: String) = AsyncStack(AuthorityKey -> Role.ADMIN) { implicit request =>
    itemService.deleteByDataset(id).map { _ =>
      Ok
    }
  }

}
