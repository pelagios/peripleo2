package controllers.api.admin

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseAuthController, HasPrettyPrintJSON, Security}
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents, Request }
import services.task.TaskService
import scala.concurrent.ExecutionContext
import services.task.TaskType
import services.user.{UserService, Role}

@Singleton
class TaskAPIController @Inject() (
  val components: ControllerComponents,
  val config: Configuration,
  val users: UserService,
  val taskService: TaskService,
  val silhouette: Silhouette[Security.Env], 
  implicit val ctx: ExecutionContext
) extends BaseAuthController(components) with HasPrettyPrintJSON {

  def list(typeFilter: Option[String], offset: Int, limit: Int) = silhouette.SecuredAction(Security.WithRole(Role.ADMIN)).async { implicit request =>
    val f = typeFilter match {
      case Some(taskType) =>
        taskService.findByType(TaskType(taskType), offset, limit)
        
      case None =>
        taskService.listAll(offset, limit)
    }
    
    f.map { page => jsonOk(Json.toJson(page)) }
  }
  
}