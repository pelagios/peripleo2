package controllers

import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, Controller}
import scala.concurrent.ExecutionContext
import services.item.{ItemService, ItemType}
import services.item.search._
import services.visit.{VisitService, TimeInterval} 
import org.joda.time.Period

@Singleton
class ApplicationController @Inject() (
  val searchService: SearchService,
  implicit val itemService: ItemService,
  implicit val visitService: VisitService,
  implicit val ctx: ExecutionContext,
  implicit val webjars: WebJarAssets
) extends Controller with HasVisitLogging {

  def index = Action.async { implicit request =>
    val fTimerange =
      searchService.getTimerange(SearchArgs(None, 0, 0, SearchFilters.NO_FILTERS, ResponseSettings.DEFAULT))
    val fDatasets =
      itemService.findByType(ItemType.DATASET, true, 0, 0)
    val fVisitStats= 
      visitService.getStatsSince(TimeInterval.LAST_7DAYS)
    
    val f = for {
      t <- fTimerange
      datasets <- fDatasets
      visitStats <- fVisitStats
    } yield (t.totalHits, datasets.total, new Period(t.from, t.to), visitStats.topItems.headOption.map(_._1.identifier))
    
    f.map { case (itemCount, datasetCount, timerange, topItemId) =>
      Ok(views.html.landing.index(itemCount, datasetCount, timerange.getYears, topItemId))
    }
  }
  
  def embed(identifier: String) = Action.async { implicit request =>
    itemService.findByIdentifier(identifier).map {
      case Some(item) =>
        logEmbed(item)
        Ok(views.html.embed.index(item))
        
      case None => NotFound
    }
  }

  def ui = Action { implicit request =>
    Ok(views.html.ui.index())
  }
  
}
