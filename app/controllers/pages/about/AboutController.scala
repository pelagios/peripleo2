package controllers.pages.about

import controllers.{HasVisitLogging, WebJarAssets}
import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, Controller}
import services.visit.VisitService

@Singleton
class AboutController @Inject()(
  implicit val visitService: VisitService,
  implicit val webjars: WebJarAssets
) extends Controller with HasVisitLogging {
  
  def index = Action { implicit request =>
    logPageView()
    Ok(views.html.pages.about.index())
  }
  
}