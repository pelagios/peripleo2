package controllers

import javax.inject.{ Inject, Singleton }
import play.api.mvc.{ Action, Controller }
import scala.concurrent.ExecutionContext
import services.item.{ ItemService, ItemType }
import services.item.search._

@Singleton
class ApplicationController @Inject() (
  val itemService: ItemService,
  val searchService: SearchService,
  implicit val ctx: ExecutionContext,
  implicit val webjars: WebJarAssets
) extends Controller {

  def index() = Action.async { implicit request =>    
    val fItems =
      searchService.query(SearchArgs(None, 0, 0, SearchFilters.NO_FILTERS, ResponseSettings.DEFAULT))
    
    val fDatasets =
      itemService.findByType(ItemType.DATASET, true, 0, 0)
    
    val f = for {
      items <- fItems
      datasets <- fDatasets
    } yield (items.total, datasets.total)
    
    f.map { case (itemCount, datasetCount) =>
      Ok(views.html.landing.index(itemCount, datasetCount))
    }
  }

  def ui() = Action { implicit request =>
    Ok(views.html.ui.index())
  }
  
}
