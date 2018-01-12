package controllers.admin.maintenance

import controllers.{BaseAuthController, WebJarAssets}
import javax.inject.{Inject, Singleton}
import jp.t2v.lab.play2.auth.AuthElement
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.mvc.Action
import play.api.libs.json.Json
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Try, Success, Failure}
import services.user.{Role, UserService}
import services.item.{Item, ItemService}

@Singleton
class MaintenanceShaftController @Inject() (
  val config: Configuration,
  val users: UserService,
  val itemService: ItemService,
  implicit val ctx: ExecutionContext,
  implicit val webjars: WebJarsUtil
) extends BaseAuthController with AuthElement {
  
  def index = StackAction(AuthorityKey -> Role.ADMIN) { implicit request =>
    Ok(views.html.admin.maintenance.index())
  }
  
  def updateItem = AsyncStack(AuthorityKey -> Role.ADMIN) { implicit request =>
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
