package controllers.api.legacy

import controllers.api.legacy.response._
import controllers.{HasPrettyPrintJSON, HasVisitLogging}
import javax.inject.{Inject, Singleton}
import play.api.mvc.{AnyContent, AbstractController, ControllerComponents, Request}
import play.api.libs.json.{Json, JsValue}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import services.Page
import services.item.{ItemService, ItemType, PathHierarchy}
import services.item.search.{ResponseSettings, SearchArgs, SearchService, SearchFilters}
import services.item.search.filters.{ReferencedItemFilter, TermFilter}
import services.visit.VisitService

@Singleton
class LegacyAPIController @Inject() (
  val components: ControllerComponents,
  implicit val itemService: ItemService,
  implicit val searchService: SearchService,
  implicit val visitService: VisitService,
  implicit val ctx: ExecutionContext
) extends AbstractController(components) with HasVisitLogging {
  
  private val CORS_ENABLED = true // TODO make configurable
  
  private def jsonOk(obj: JsValue)(implicit request: Request[AnyContent]) = {
    val headers = if (CORS_ENABLED) Seq(("Access-Control-Allow-Origin" -> "*")) else Seq.empty[(String, String)]
      
    val pretty = Try(request.queryString.get("prettyprint").map(_.head.toBoolean).getOrElse(false)).getOrElse(false)
    if (pretty) 
      Ok(Json.prettyPrint(obj)).as("application/json").withHeaders(headers:_*)
    else 
      Ok(obj).withHeaders(headers:_*)
  }
    
  def search() = Action.async { implicit request =>
    val args = LegacySearchArgs.fromQueryString(request.queryString)
    searchService.query(args).map { r => 
      logSearch(args, r)      
      val legacyFormat = 
        Page(r.took, r.total, r.offset, r.limit, r.items.map(LegacySearchResult.fromItem(_)))
      jsonOk(Json.toJson(legacyFormat))
    }
  }
  
  def getItem(id: String) = Action.async { implicit request =>
    itemService.findByIdentifier(id).flatMap {
      case Some(item) =>
        val fReferenceCount = itemService.countReferences(id)
        val fPlaceCount = itemService.countReferencesToType(id, ItemType.PLACE)
        val f = for {
          refCount <- fReferenceCount
          placeCount <- fPlaceCount
        } yield (refCount._1, placeCount._2)
        
        f.map { case (references, places) =>
          jsonOk(Json.toJson(LegacyItem.fromItem(item, references, places)))
        }
            
      case None =>
        Future.successful(NotFound)
    }
  }
  
  def getPlace(id: String) = Action.async { implicit request =>
    
    // Query returns the items that reference the place, plus aggregation by datasets 
    val args = SearchArgs(
      None, // query
      0,    // limit
      0,    // offset
      SearchFilters.NO_FILTERS.copy(referencedItemFilter =
        Some(ReferencedItemFilter(Seq(id), TermFilter.ONLY))),
      ResponseSettings.DEFAULT.copy(termAggregations = true))
   
    val fItem = itemService.findByIdentifier(id)    
    val fReferencedIn = searchService.query(args)
    
    val f = for {
      item         <- fItem
      referencedIn <- fReferencedIn
    } yield (item, referencedIn)
    
    f.map { 
      case (Some(item), referencedIn) =>
        val topLevelCounts = referencedIn.aggregations
          .find(_.name == "by_dataset").map(_.buckets)
          .getOrElse(Seq.empty[(String, Long)])
          .filter(!_._1.contains(PathHierarchy.OUTER_SEPARATOR))
          
        // Base URL to use for the 'peripleo_url' response field
        val base = controllers.routes.ApplicationController.ui.absoluteURL()
        jsonOk(Json.toJson(LegacyPlace(item, topLevelCounts, base)))

      case (None, _) => NotFound
    }
  }

}