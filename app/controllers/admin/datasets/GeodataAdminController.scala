package controllers.admin.datasets

import controllers.{ BaseController, WebJarAssets }
import javax.inject.{ Inject, Singleton }
import jp.t2v.lab.play2.auth.AuthElement
import play.api.Configuration
import play.api.mvc.Action
import services.user.{ Role, UserService }

@Singleton
class GeodataAdminController @Inject() (
  val config: Configuration,
  val users: UserService,
  implicit val webjars: WebJarAssets
) extends BaseController with AuthElement {

  def index = StackAction(AuthorityKey -> Role.ADMIN) { implicit request =>
    Ok(views.html.admin.datasets.geodata())
  }

}
