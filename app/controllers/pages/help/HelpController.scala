package controllers.pages.help

import controllers.{HasVisitLogging, WebJarAssets}
import javax.inject.{Inject, Singleton}
import org.webjars.play.WebJarsUtil
import play.api.mvc.{Action, Controller}
import services.visit.VisitService

@Singleton
class HelpController @Inject()(
  implicit val visitService: VisitService,
  implicit val webjars: WebJarsUtil
) extends Controller with HasVisitLogging {

  def index = Action { implicit request =>
    Redirect(routes.HelpController.introduction)
  }

  def introduction = Action { implicit request =>
    logPageView()
    Ok(views.html.pages.help.introduction())
  }

  def entityAwareSearch = Action { implicit request =>
    logPageView()
    Ok(views.html.pages.help.entityAwareSearch())
  }

  def relatedEntities = Action { implicit request =>
    logPageView()
    Ok(views.html.pages.help.relatedEntities())
  }

  def linkedDataView = Action { implicit request =>
    logPageView()
    Ok(views.html.pages.help.linkedDataView())
  }

  def embedWidget = Action { implicit request =>
    logPageView()
    Ok(views.html.pages.help.embedWidget())
  }

}
