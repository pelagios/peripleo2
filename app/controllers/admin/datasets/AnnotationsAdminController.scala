package controllers.admin.datasets

import controllers.{ BaseController, WebJarAssets }
import controllers.admin.DumpImporter
import javax.inject.{ Inject, Singleton }
import jp.t2v.lab.play2.auth.AuthElement
import play.api.{ Configuration, Logger }
import play.api.mvc.Action
import services.task.TaskService
import services.user.{ Role, UserService }

@Singleton
class AnnotationsAdminController @Inject() (
  val config: Configuration,
  val taskService: TaskService,
  val users: UserService,
  implicit val webjars: WebJarAssets
) extends BaseController with AuthElement {

  def index = StackAction(AuthorityKey -> Role.ADMIN) { implicit request =>
    Ok(views.html.admin.datasets.annotations())
  }

  def importAnnotations = StackAction(AuthorityKey -> Role.ADMIN) { implicit request =>
    request.body.asMultipartFormData.flatMap(_.file("file")) match {
      case Some(formData) =>

        // DUMMY for testing only!

        if (formData.filename.contains(".rdf")) {
          Logger.info("Importing Pelagios RDF/XML dump")
          val importer = new DumpImporter(taskService)
          // importer.importDump(formData.ref.file, formData.filename, PelagiosRDFCrosswalk.fromRDF(formData.filename), placeService, loggedIn.username)
        }

        Redirect(routes.AnnotationsAdminController.index)

      case None => BadRequest
    }
  }

}
