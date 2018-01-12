package controllers.admin.authorities

import akka.actor.ActorSystem
import controllers.WebJarAssets
import harvesting.loaders.DumpLoader
import harvesting.crosswalks.periods.PeriodoCrosswalk
import javax.inject.{ Inject, Singleton }
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import scala.concurrent.ExecutionContext
import services.user.{ Role, UserService }
import services.task.{ TaskService, TaskType }
import services.item.{ ItemService, ItemType, PathHierarchy }
import services.item.importers.{ DatasetImporter, EntityImporter }

@Singleton
class PeriodAdminController @Inject() (
  val config: Configuration,
  val users: UserService,
  val itemService: ItemService,
  val taskService: TaskService,
  implicit val ctx: ExecutionContext,
  implicit val system: ActorSystem,
  implicit val webjars: WebJarsUtil
) extends BaseAuthorityAdminController(new DatasetImporter(itemService, ItemType.DATASET.AUTHORITY.PERIODS)) {

  def index = StackAction(AuthorityKey -> Role.ADMIN) { implicit request =>
    Ok(views.html.admin.authorities.periods())
  }
  
  def importAuthorityFile = StackAction(AuthorityKey -> Role.ADMIN) { implicit request =>
    request.body.asMultipartFormData.flatMap(_.file("file")) match {
      case Some(formdata) =>
        val importer = new EntityImporter(itemService, ItemType.PERIOD)
        
        // TODO temporary hack - should have proper URI + title
        val name = formdata.filename.substring(0, formdata.filename.indexOf('.'))
        val dataset = PathHierarchy(name, name)
        
        upsertDatasetRecord(name, name).map { success =>
          new DumpLoader(taskService, TaskType("AUTHORITY_IMPORT_PERIODS")).importDump(
            formdata.filename,
            formdata.ref.file,
            formdata.filename,
            PeriodoCrosswalk.fromJSON(formdata.filename, dataset),
            importer,
            loggedIn.username)            
        }

        Redirect(routes.PeopleAdminController.index)
        
      case None => BadRequest
    }
  }

}