package controllers.admin

import com.mohiva.play.silhouette.api.{LoginInfo, Silhouette}
import controllers.{HasConfig, HasUserService, Security}
import javax.inject.Inject
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.I18nSupport
import play.api.mvc.{AbstractController, ControllerComponents}
import scala.concurrent.{ExecutionContext, Future}
import services.user.UserService

case class LoginData(usernameOrPassword: String, password: String)

class LoginLogoutController @Inject() (
  val components: ControllerComponents,
  val config: Configuration,
  val users: UserService,
  val silhouette: Silhouette[Security.Env],
  implicit val ctx: ExecutionContext
) extends AbstractController(components) with HasConfig with HasUserService with I18nSupport {

  private val MESSAGE = "message"

  private val INVALID_LOGIN = "Invalid Username or Password"
  
  private val auth = silhouette.env.authenticatorService

  val loginForm = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText
    )(LoginData.apply)(LoginData.unapply)
  )

  def showLoginForm(destination: Option[String]) = Action { implicit request =>
    destination match {
      case None => Ok(views.html.admin.login(loginForm))
      case Some(dest) => Ok(views.html.admin.login(loginForm)).withSession("access_uri" -> dest)
    }
  }

  def processLogin = Action.async { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors =>
        Future(BadRequest(views.html.admin.login(formWithErrors))),

      loginData =>
        users.validateUser(loginData.usernameOrPassword, loginData.password).flatMap {
          case Some(validUser) => 
            val destination = request.session.get("access_uri").getOrElse(controllers.admin.routes.AdminController.index.toString)
            auth.create(LoginInfo(Security.PROVIDER_ID, validUser.username))
              .flatMap(auth.init(_))
              .flatMap(auth.embed(_,
                Redirect(destination).withSession(request.session - "access_uri")
              ))
            
          case None => Future(Redirect(routes.LoginLogoutController.showLoginForm()).flashing(MESSAGE -> INVALID_LOGIN))
        }
    )
  }

  def logout = silhouette.SecuredAction.async { implicit request =>
    auth.discard(request.authenticator, Redirect(controllers.routes.ApplicationController.landing))
  }

}