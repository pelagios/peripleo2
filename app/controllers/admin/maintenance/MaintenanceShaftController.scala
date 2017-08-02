package controllers.admin.maintenance

import controllers.{ BaseAuthController, WebJarAssets }
import javax.inject.{ Inject, Singleton }
import jp.t2v.lab.play2.auth.AuthElement
import play.api.Configuration
import play.api.mvc.Action
import services.user.{ Role, UserService }

@Singleton
class MaintenanceShaftController @Inject() (
  val config: Configuration,
  val users: UserService,
  implicit val webjars: WebJarAssets
) extends BaseAuthController with AuthElement {
  
  def index = StackAction(AuthorityKey -> Role.ADMIN) { implicit request =>
    Ok(views.html.admin.maintenance.index())
  }
  
}
