package controllers.pages.apidocs

import controllers.{HasVisitLogging, WebJarAssets}
import javax.inject.{Inject, Singleton}
import org.webjars.play.WebJarsUtil
import play.api.mvc.{Action, Controller}
import services.visit.VisitService

@Singleton
class APIDocsController @Inject()(
  implicit val visitService: VisitService,
  implicit val webjars: WebJarsUtil
) extends Controller with HasVisitLogging {

  def index = Action { implicit request =>
    logPageView()
    Ok(views.html.pages.apidocs.index())
  }

}
