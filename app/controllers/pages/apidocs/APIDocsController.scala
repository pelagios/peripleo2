package controllers.pages.apidocs

import controllers.HasVisitLogging
import javax.inject.{Inject, Singleton}
import org.webjars.play.WebJarsUtil
import play.api.mvc.{AbstractController, ControllerComponents}
import services.visit.VisitService

@Singleton
class APIDocsController @Inject()(
  val components: ControllerComponents,
  implicit val visitService: VisitService,
  implicit val webjars: WebJarsUtil
) extends AbstractController(components) with HasVisitLogging {

  def index = Action { implicit request =>
    logPageView()
    Ok(views.html.pages.apidocs.index())
  }

}
