package controllers.api.admin

import controllers.{ BaseAuthController, HasPrettyPrintJSON }
import javax.inject.{ Inject, Singleton }
import jp.t2v.lab.play2.auth.AuthElement
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{ Action, AnyContent, Controller, Request }
import services.task.TaskService
import scala.concurrent.ExecutionContext
import services.task.TaskType
import services.user.{ UserService, Role }

@Singleton
class TaskAPIController @Inject() (
  val config: Configuration,
  val users: UserService,
  val taskService: TaskService,
  implicit val ctx: ExecutionContext
) extends BaseAuthController with AuthElement with HasPrettyPrintJSON {
  
  private def getArg(key: String)(implicit request: Request[AnyContent]): Option[String] =
    request.queryString.get(key).flatMap(_.headOption)
  
  def list() = AsyncStack(AuthorityKey -> Role.ADMIN) { implicit request =>
    val offset = getArg("offset").map(_.toInt).getOrElse(0)
    val limit = getArg("limit").map(_.toInt).getOrElse(20)
    val typeFilter = getArg("type")
    
    val f = typeFilter match {
      case Some(taskType) =>
        taskService.findByType(TaskType(taskType), offset, limit)
        
      case None =>
        taskService.listAll(offset, limit)
    }
    
    f.map { page => jsonOk(Json.toJson(page)) }
  }
  
}