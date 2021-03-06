package controllers

import javax.inject.{Inject,Singleton}
import play.api.http.HeaderNames
import play.api.mvc.{AbstractController, ControllerComponents}
import org.joda.time.Period
import org.webjars.play.WebJarsUtil
import scala.concurrent.{Future,ExecutionContext}
import services.item.{ItemService,ItemType}
import services.item.search._
import services.visit.{VisitService,TimeInterval}

@Singleton
class ApplicationController @Inject() (
  val components: ControllerComponents,
  val searchService: SearchService,
  implicit val itemService: ItemService,
  implicit val visitService: VisitService,
  implicit val ctx: ExecutionContext,
  implicit val webjars: WebJarsUtil
) extends AbstractController(components) with HasVisitLogging {

  def landing = Action.async { implicit request =>
    logPageView()

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
    itemService.findByIdentifier(identifier).flatMap {
      case Some(item) =>
        // Don't log embeds requests coming from the splash page
        val referrer = request.headers.get(HeaderNames.REFERER)
        val splashURL = routes.ApplicationController.landing().absoluteURL()
        if (referrer != Some(splashURL))
          logEmbed(item)

        item.representativePoint match {
          case Some(geom) =>
            // This item comes with its own geometry
            Future.successful(Ok(views.html.embed.index(item, None)))

          case None =>
            // No geometry? Fetch top N places referenced by this item instead
            itemService.getTopReferenced(item.identifiers.head).map { t =>
              Ok(views.html.embed.index(item, Some(t)))
            }
        }

      case None => Future.successful(NotFound)
    }
  }

  def ui = Action { implicit request =>
    Ok(views.html.ui.index())
  }

  def legacyRedirect = Action { implicit request =>
    Ok(views.html.legacy_redirect())
  }

  def legacyRedirectAnyPath(path: String) = legacyRedirect

}
