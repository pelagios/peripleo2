package controllers.pages.about

import controllers.HasVisitLogging
import es.ES
import javax.inject.{Inject, Singleton}
import org.webjars.play.WebJarsUtil
import play.api.mvc.{AbstractController, ControllerComponents}
import scala.concurrent.ExecutionContext
import services.Sort
import services.item.{ItemService, ItemType}
import services.visit.VisitService

@Singleton
class AboutController @Inject()(
  val components: ControllerComponents,
  implicit val itemService: ItemService,
  implicit val visitService: VisitService,
  implicit val ctx: ExecutionContext,
  implicit val webjars: WebJarsUtil
) extends AbstractController(components) with HasVisitLogging {

  def index = Action.async { implicit request =>
    logPageView()
    itemService.findByType(ItemType.DATASET, true, 0, ES.MAX_SIZE, Some(Sort.ALPHABETICAL)).map { d =>
      Ok(views.html.pages.about.index(d))
    }
  }

}
