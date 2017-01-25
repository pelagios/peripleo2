package controllers.admin.notifications

import controllers.BaseController
import javax.inject.{ Inject, Singleton }
import jp.t2v.lab.play2.auth.AuthElement
import play.api.Configuration
import play.api.mvc.Action
import services.user.{ Role, UserService }

@Singleton
class NotificationAdminController @Inject() (
  val config: Configuration,
  val users: UserService
) extends BaseController with AuthElement {

  def index = StackAction(AuthorityKey -> Role.ADMIN) { implicit request =>
    Ok(views.html.admin.notifications.index())
  }

}
