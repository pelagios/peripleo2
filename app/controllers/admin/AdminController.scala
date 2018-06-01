package controllers.admin

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseAuthController, Security}
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.mvc.ControllerComponents
import scala.concurrent.ExecutionContext
import services.item.export.DumpExporter
import services.user.{Role, UserService}
import es.ES

@Singleton
class AdminController @Inject() (
  val components: ControllerComponents,
  val config: Configuration,
  val exporter: DumpExporter,
  val users: UserService,
  val silhouette: Silhouette[Security.Env],
  implicit val ctx: ExecutionContext,
  implicit val es: ES  
) extends BaseAuthController(components) {

  def index = silhouette.SecuredAction(Security.WithRole(Role.ADMIN)) { implicit request =>
    Redirect(controllers.admin.datasets.routes.GeodataAdminController.index)
  }

  def datasets = silhouette.SecuredAction(Security.WithRole(Role.ADMIN)) { implicit request =>
    Redirect(controllers.admin.datasets.routes.GeodataAdminController.index)
  }

  def authorities = silhouette.SecuredAction(Security.WithRole(Role.ADMIN)) { implicit request =>
    Redirect(controllers.admin.authorities.routes.GazetteerAdminController.index)
  }

  def dumpAll = silhouette.SecuredAction(Security.WithRole(Role.ADMIN)) { implicit request  =>
    exporter.exportAll()
    Ok
  }

}
