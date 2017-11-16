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
    logPageView()
    Ok(views.html.pages.help.index())
  }
  
  def embed = Action { implicit request =>
    logPageView()
    Ok(views.html.pages.help.embed())
  }
  
}