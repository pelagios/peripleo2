package controllers.admin.authorities

import akka.actor.ActorSystem
import akka.stream.Materializer
import controllers.WebJarAssets
import harvesting.loaders.StreamLoader
import javax.inject.{ Inject, Singleton }
import play.api.Configuration
import scala.concurrent.ExecutionContext
import services.user.{ Role, UserService }
import services.item.{ ItemService, ItemType }
import services.item.importers.{ DatasetImporter, EntityImporter }
import services.task.{ TaskService, TaskType }
import harvesting.crosswalks.people.SimplePeopleCrosswalk

@Singleton
class PeopleAdminController @Inject() (
  val config: Configuration,
  val users: UserService,
  val itemService: ItemService,
  val taskService: TaskService,
  val materializer: Materializer,
  implicit val ctx: ExecutionContext,
  implicit val system: ActorSystem,
  implicit val webjars: WebJarAssets
) extends BaseAuthorityAdminController(new DatasetImporter(itemService)) {

  def index = StackAction(AuthorityKey -> Role.ADMIN) { implicit request =>
    Ok(views.html.admin.authorities.people())
  }
  
  def importAuthorityFile = StackAction(AuthorityKey -> Role.ADMIN) { implicit request =>
    request.body.asMultipartFormData.flatMap(_.file("file")) match {
      case Some(formdata) =>
        val importer = new EntityImporter(itemService, ItemType.PERSON)
        
        // TODO temporary hack - should have proper URI + title
        val name = formdata.filename.substring(0, formdata.filename.indexOf('.'))
        
        upsertDatasetRecord(ItemType.DATASET.AUTHORITY.PEOPLE, name, name).map { success =>
          new StreamLoader(taskService, TaskType("AUTHORITY_IMPORT_PEOPLE"), materializer).importRecords(
            formdata.filename,
            formdata.ref.file,
            formdata.filename,
            SimplePeopleCrosswalk.fromJson,
            importer,
            loggedIn.username)
        }

        Redirect(routes.PeopleAdminController.index)
        
      case None => BadRequest
    }
  }

}