package controllers.admin

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseAuthController, Security}
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.mvc.ControllerComponents
import services.user.{Role, UserService}

@Singleton
class AdminController @Inject() (
  val components: ControllerComponents,
  val config: Configuration,
  val users: UserService,
  val silhouette: Silhouette[Security.Env]
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

}
