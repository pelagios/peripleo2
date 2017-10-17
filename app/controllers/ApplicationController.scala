package controllers

import javax.inject.{ Inject, Singleton }
import play.api.mvc.{ Action, Controller }
import scala.concurrent.ExecutionContext
import services.item.{ ItemService, ItemType }
import services.item.search._
import org.joda.time.Period

@Singleton
class ApplicationController @Inject() (
  val itemService: ItemService,
  val searchService: SearchService,
  implicit val ctx: ExecutionContext,
  implicit val webjars: WebJarAssets
) extends Controller {

  def index = Action.async { implicit request =>
    val fTimerange =
      searchService.getTimerange(SearchArgs(None, 0, 0, SearchFilters.NO_FILTERS, ResponseSettings.DEFAULT))
    val fDatasets =
      itemService.findByType(ItemType.DATASET, true, 0, 0)
          
    val f = for {
      t <- fTimerange
      datasets <- fDatasets
    } yield (t.totalHits, datasets.total, new Period(t.from, t.to))
    
    f.map { case (itemCount, datasetCount, timerange) =>
      Ok(views.html.landing.index(itemCount, datasetCount, timerange.getYears))
    }
  }

  def ui = Action { implicit request =>
    Ok(views.html.ui.index())
  }
  
}
