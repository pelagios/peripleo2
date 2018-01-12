package controllers.admin.maintenance

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseAuthController, Security}
import javax.inject.{Inject, Singleton}
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.mvc.ControllerComponents
import play.api.libs.json.Json
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Try, Success, Failure}
import services.user.{Role, UserService}
import services.item.{Item, ItemService}

@Singleton
class MaintenanceShaftController @Inject() (
  val components: ControllerComponents,
  val config: Configuration,
  val users: UserService,
  val itemService: ItemService,
  val silhouette: Silhouette[Security.Env],
  implicit val ctx: ExecutionContext,
  implicit val webjars: WebJarsUtil
) extends BaseAuthController(components) {

  def index = silhouette.SecuredAction(Security.WithRole(Role.ADMIN)) { implicit request =>
    Ok(views.html.admin.maintenance.index())
  }

  def updateItem = silhouette.SecuredAction(Security.WithRole(Role.ADMIN)).async { implicit request =>
    request.body.asFormUrlEncoded.get("item").headOption match {
      case Some(json) =>
        Try(Json.fromJson[Item](Json.parse(json)).get) match {
          case Success(item) =>
            itemService.updateItem(item).map { success =>
              if (success) Ok
              else InternalServerError
            }

          case Failure(_) => Future.successful(BadRequest)
        }

      case None => Future.successful(BadRequest)
    }
  }

}
