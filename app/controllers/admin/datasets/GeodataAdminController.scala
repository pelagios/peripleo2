package controllers.admin.datasets

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseAuthController, Security}
import javax.inject.{Inject, Singleton}
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.mvc.ControllerComponents
import services.user.{Role, UserService}

@Singleton
class GeodataAdminController @Inject() (
  val components: ControllerComponents,
  val config: Configuration,
  val users: UserService,
  val silhouette: Silhouette[Security.Env],
  implicit val webjars: WebJarsUtil
) extends BaseAuthController(components) {

  def index = silhouette.SecuredAction(Security.WithRole(Role.ADMIN)) { implicit request =>
    Ok(views.html.admin.datasets.geodata())
  }

}
