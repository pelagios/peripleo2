package controllers.admin

import controllers.BaseAuthController
import javax.inject.{ Inject, Singleton }
import jp.t2v.lab.play2.auth.AuthElement
import play.api.Configuration
import play.api.mvc.Action
import services.user.{ Role, UserService }

@Singleton
class AdminController @Inject() (
  val config: Configuration,
  val users: UserService
) extends BaseAuthController with AuthElement {

  def index = StackAction(AuthorityKey -> Role.ADMIN) { implicit request =>
    Redirect(controllers.admin.datasets.routes.GeodataAdminController.index)
  }

  def datasets = StackAction(AuthorityKey -> Role.ADMIN) { implicit request =>
    Redirect(controllers.admin.datasets.routes.GeodataAdminController.index)
  }

  def authorities = StackAction(AuthorityKey -> Role.ADMIN) { implicit request =>
    Redirect(controllers.admin.authorities.routes.GazetteerAdminController.index)
  }

}
