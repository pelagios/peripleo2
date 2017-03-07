package controllers.admin.datasets

import akka.actor.ActorSystem
import controllers.{ BaseController, WebJarAssets }
import controllers.admin.DumpImporter
import harvesting.VoIDHarvester
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
  val voidHarvester: VoIDHarvester,
  implicit val ctx: ExecutionContext,
  implicit val system: ActorSystem,
  implicit val webjars: WebJarAssets
) extends BaseController with AuthElement {

  def index = StackAction(AuthorityKey -> Role.ADMIN) { implicit request =>
    Ok(views.html.admin.datasets.annotations())
  }
  
  def importVoID = StackAction(AuthorityKey -> Role.ADMIN) { implicit request =>
    request.body.asFormUrlEncoded match {
      case Some(form) => form.get("url").flatMap(_.headOption) match {
        case Some(url) =>
          voidHarvester.harvest(url, loggedIn.username).map { success =>
            if (success) Logger.info(s"Import complete from $url")
            else Logger.error(s"Import failed from $url")
          } recover { case t: Throwable =>
            t.printStackTrace
            Logger.info("Error harvesting VoID: " + t.getMessage)
          }
          
          Ok // Return immediately
          
        case None => BadRequest
      }
        
      case None => BadRequest
    }
  }
  
  def deleteDataset(id: String) = AsyncStack(AuthorityKey -> Role.ADMIN) { implicit request =>
    itemService.deleteByDataset(id).map { _ =>
      Ok
    }
  }
  
  // def importVoID = StackAction(AuthorityKey -> Role.ADMIN) { implicit request =>
  //    
  //   request.body.asMultipartFormData.flatMap(_.file("file")) match {
  //     case Some(formData) =>
  //       if (formData.filename.contains(".rdf")) {
  //         Logger.info("Importing Pelagios VoID")
  //         val importer = new DumpImporter(taskService)
  //         importer.importDump(formData.ref.file, formData.filename, PelagiosVoIDCrosswalk.fromRDF(formData.filename), itemService, loggedIn.username)
  //       }
  //      
  //       Redirect(routes.AnnotationsAdminController.index)
  //     
  //     case None => BadRequest
  //   }
  // }

  // def importAnnotations = StackAction(AuthorityKey -> Role.ADMIN) { implicit request =>
  //   request.body.asMultipartFormData.flatMap(_.file("file")) match {
  //     case Some(formData) =>
  // 
  //       if (formData.filename.contains(".rdf")) {
  //         Logger.info("Importing Pelagios RDF/XML dump")
  //         val importer = new DumpImporter(taskService)
  //         importer.importDump(formData.ref.file, formData.filename, PelagiosAnnotationCrosswalk.fromRDF(formData.filename), itemService, loggedIn.username)
  //       }
  // 
  //       Redirect(routes.AnnotationsAdminController.index)
  // 
  //     case None => BadRequest
  //   }
  // }

}
