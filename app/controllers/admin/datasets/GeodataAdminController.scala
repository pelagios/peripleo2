package controllers.admin.datasets

import controllers.{BaseAuthController, WebJarAssets}
import javax.inject.{Inject, Singleton}
import jp.t2v.lab.play2.auth.AuthElement
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.mvc.Action
import services.user.{Role, UserService}

@Singleton
class GeodataAdminController @Inject() (
  val config: Configuration,
  val users: UserService,
  implicit val webjars: WebJarsUtil
) extends BaseAuthController with AuthElement {

  def index = StackAction(AuthorityKey -> Role.ADMIN) { implicit request =>
    Ok(views.html.admin.datasets.geodata())
  }

}
