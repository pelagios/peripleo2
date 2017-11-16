package controllers.pages.about

import controllers.{HasVisitLogging, WebJarAssets}
import es.ES
import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, Controller}
import scala.concurrent.ExecutionContext
import services.Sort
import services.item.{ItemService, ItemType}
import services.visit.VisitService

@Singleton
class AboutController @Inject()(
  implicit val itemService: ItemService,
  implicit val visitService: VisitService,
  implicit val ctx: ExecutionContext,
  implicit val webjars: WebJarAssets
) extends Controller with HasVisitLogging {
  
  def index = Action.async { implicit request =>
    logPageView()
    itemService.findByType(ItemType.DATASET, true, 0, ES.MAX_SIZE, Some(Sort.ALPHABETICAL)).map { d =>
      Ok(views.html.pages.about.index(d)) 
    }
  }
  
}