package controllers.pages.help

import controllers.{HasVisitLogging, WebJarAssets}
import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, Controller}
import services.visit.VisitService

@Singleton
class HelpController @Inject()(
  implicit val visitService: VisitService,
  implicit val webjars: WebJarAssets
) extends Controller with HasVisitLogging {
  
  def index = Action { implicit request =>
    Redirect(routes.HelpController.introduction)
  }
  
  def introduction = Action { implicit request =>
    logPageView()
    Ok(views.html.pages.help.introduction())
  }

  def linkedDataView = Action { implicit request =>
    logPageView()
    Ok(views.html.pages.help.linkedDataView())
  }

  def fulltextSearch = Action { implicit request =>
    logPageView()
    Ok(views.html.pages.help.fulltextSearch())
  }
    
  def embed = Action { implicit request =>
    logPageView()
    Ok(views.html.pages.help.embed())
  }
  
}