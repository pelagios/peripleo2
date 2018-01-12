package controllers.pages.help

import controllers.HasVisitLogging
import javax.inject.{Inject, Singleton}
import org.webjars.play.WebJarsUtil
import play.api.mvc.{AbstractController, ControllerComponents}
import services.visit.VisitService

@Singleton
class HelpController @Inject()(
  val components: ControllerComponents,
  implicit val visitService: VisitService,
  implicit val webjars: WebJarsUtil
) extends AbstractController(components) with HasVisitLogging {

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
