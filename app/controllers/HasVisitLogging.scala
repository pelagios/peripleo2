package controllers

import eu.bitwalker.useragentutils.UserAgent
import org.joda.time.DateTime
import play.api.Logger
import play.api.mvc.RequestHeader
import play.api.http.HeaderNames
import scala.concurrent.{ExecutionContext, Future}
import services.visit._
import services.visit.info._
import services.item.search.{SearchArgs, RichResultPage}
import services.item.{Item, ItemType, ItemService}

trait HasVisitLogging {
    
  private def baseVisit(request: RequestHeader, visitType: VisitType.Value): Visit = {
    val userAgentHeader = request.headers.get(HeaderNames.USER_AGENT)
    val userAgent = userAgentHeader.map(ua => UserAgent.parseUserAgentString(ua))
    val os = userAgent.map(_.getOperatingSystem)
    
    Visit(
      request.uri,
      request.headers.get(HeaderNames.REFERER),
      DateTime.now,
      visitType,
      Client(
        request.remoteAddress,
        userAgentHeader.getOrElse("UNKNOWN"),
        userAgent.map(_.getBrowser.getGroup.getName).getOrElse("UNKNOWN"),
        os.map(_.getName).getOrElse("UNKNOWN"),
        os.map(_.getDeviceType.getName).getOrElse("UNKNOWN")  
      ),
      None, None, None)
  }
  
  private def log(v: Visit)(implicit visitService: VisitService) = 
    if (HasVisitLogging.isBot(v)) Future.successful(())
    else visitService.insertVisit(v)
  
  protected def logSearch(args: SearchArgs, response: RichResultPage)(implicit request: RequestHeader, visitService: VisitService, ctx: ExecutionContext) = 
    Future {
      def top(t: ItemType) = response.topReferenced.map(_.count(t)).getOrElse(0)
      
      val search = SearchInfo(
        args.query,
        SearchInfo.Returned(
          response.total,
          top(ItemType.PLACE),
          top(ItemType.PERSON),
          top(ItemType.PERIOD)
        ))
      
      val v = baseVisit(request, VisitType.SEARCH)
      v.copy(
        responseTime = Some(response.took),
        search = Some(search))
    } flatMap { 
      log(_) 
    }
  
  protected def logSelection(identifier: String)(implicit request: RequestHeader, itemService: ItemService, visitService: VisitService, ctx: ExecutionContext) =
    itemService.findByIdentifier(identifier).flatMap {
      case Some(item) =>
        val firstRecord = item.isConflationOf.head
        val selection = SelectionInfo(identifier, item.title, firstRecord.isInDataset.get)
        val v = baseVisit(request, VisitType.SELECTION)
          .copy(selection = Some(selection))
        log(v)
        
      case None =>
        Logger.warn(s"Selection pingback for a non-existing item: ${identifier}")
        Future.successful(())
  }
  
  protected def logEmbed(item: Item)(implicit request: RequestHeader, itemService: ItemService, visitService: VisitService, ctx: ExecutionContext) = {
    val firstRecord = item.isConflationOf.head
    val info = SelectionInfo(firstRecord.uri, item.title, firstRecord.isInDataset.get)
    val v = baseVisit(request, VisitType.EMBED)
      .copy(selection = Some(info))
    log(v)
  }
  
  protected def logPageView()(implicit request: RequestHeader, visitService: VisitService) = {
    val v = baseVisit(request, VisitType.PAGE_VIEW)
    log(v)
  }

}

object HasVisitLogging {
  
  // If one of these keywords appears in the UA header, treat as bot
  private val USER_AGENT_EXCLUDES = Set("uptimerobot")
   
  def isBot(visit: Visit) =
    USER_AGENT_EXCLUDES.find(visit.client.userAgent.contains(_)).isDefined
  
}