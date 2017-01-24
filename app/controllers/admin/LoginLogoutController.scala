package controllers.admin

import controllers.{ HasConfig, HasUserService, Security }
import javax.inject.Inject
import jp.t2v.lab.play2.auth.{ AuthElement, LoginLogout }
import play.api.Configuration
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.mvc.{ Action, Controller }
import scala.concurrent.{ ExecutionContext, Future }
import services.user.UserService

case class LoginData(usernameOrPassword: String, password: String)

class LoginLogoutController @Inject() (    
    val config: Configuration,
    val users: UserService,
    val messagesApi: MessagesApi,
    implicit val ctx: ExecutionContext
  ) extends Controller with AuthElement with HasConfig with HasUserService with Security with LoginLogout with I18nSupport {

  private val MESSAGE = "message"

  private val INVALID_LOGIN = "Invalid Username or Password"

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
          case Some(validUser) => gotoLoginSucceeded(validUser.username)
          case None => Future(Redirect(routes.LoginLogoutController.showLoginForm()).flashing(MESSAGE -> INVALID_LOGIN))
        }
    )
  }

  def logout = Action.async { implicit request =>
    gotoLogoutSucceeded
  }

}