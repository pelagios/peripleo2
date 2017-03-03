package controllers.admin.datasets

import akka.actor.ActorSystem
import controllers.{ BaseController, WebJarAssets }
import controllers.admin.DumpImporter
import javax.inject.{ Inject, Singleton }
import jp.t2v.lab.play2.auth.AuthElement
import play.api.{ Configuration, Logger }
import play.api.mvc.Action
import scala.concurrent.ExecutionContext
import services.task.TaskService
import services.user.{ Role, UserService }
import services.item.ItemService
import services.item.crosswalks._

@Singleton
class AnnotationsAdminController @Inject() (
  val config: Configuration,
  val itemService: ItemService,
  val taskService: TaskService,
  val users: UserService,
  implicit val ctx: ExecutionContext,
  implicit val system: ActorSystem,
  implicit val webjars: WebJarAssets
) extends BaseController with AuthElement {

  def index = StackAction(AuthorityKey -> Role.ADMIN) { implicit request =>
    Ok(views.html.admin.datasets.annotations())
  }
  
  def importVoID = StackAction(AuthorityKey -> Role.ADMIN) { implicit request =>
    
    // DUMMY for testing only!
    
    request.body.asMultipartFormData.flatMap(_.file("file")) match {
      case Some(formData) =>
        if (formData.filename.contains(".rdf")) {
          Logger.info("Importing Pelagios VoID")
          val importer = new DumpImporter(taskService)
          importer.importDump(formData.ref.file, formData.filename, PelagiosVoIDCrosswalk.fromRDF(formData.filename), itemService, loggedIn.username)
        }
        
        Redirect(routes.AnnotationsAdminController.index)
        
      case None => BadRequest
    }
  }

  def importAnnotations = StackAction(AuthorityKey -> Role.ADMIN) { implicit request =>
    request.body.asMultipartFormData.flatMap(_.file("file")) match {
      case Some(formData) =>

        // DUMMY for testing only!

        if (formData.filename.contains(".rdf")) {
          Logger.info("Importing Pelagios RDF/XML dump")
          val importer = new DumpImporter(taskService)
          importer.importDump(formData.ref.file, formData.filename, PelagiosAnnotationCrosswalk.fromRDF(formData.filename), itemService, loggedIn.username)
        }

        Redirect(routes.AnnotationsAdminController.index)

      case None => BadRequest
    }
  }

}
