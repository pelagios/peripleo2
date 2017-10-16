package controllers

import eu.bitwalker.useragentutils.UserAgent
import org.joda.time.DateTime
import play.api.mvc.RequestHeader
import play.api.http.HeaderNames
import scala.concurrent.{ ExecutionContext, Future }
import services.visit._
import services.item.search.{ SearchArgs, RichResultPage }
import services.item.ItemType

trait HasVisitLogging {
    
  protected def logVisit(took: Option[Long], search: Option[Visit.Search]) (implicit request: RequestHeader, visitService: VisitService): Future[Unit] = {
    val userAgentHeader = request.headers.get(HeaderNames.USER_AGENT)
    val userAgent = userAgentHeader.map(ua => UserAgent.parseUserAgentString(ua))
    val os = userAgent.map(_.getOperatingSystem)
    
    val visit = Visit(
      request.uri,
      request.headers.get(HeaderNames.REFERER),
      DateTime.now(),
      Visit.Client(
        request.remoteAddress,
        userAgentHeader.getOrElse("UNKNOWN"),
        userAgent.map(_.getBrowser.getGroup.getName).getOrElse("UNKNOWN"),
        os.map(_.getName).getOrElse("UNKNOWN"),
        os.map(_.getDeviceType.getName).getOrElse("UNKNOWN")  
      ),
      took, search)

    if (HasVisitLogging.isBot(visit))
      Future.successful(())
    else
      visitService.insertVisit(visit)    
  }
  
  protected def logSearchResponse(args: SearchArgs, response: RichResultPage)(implicit request: RequestHeader, visitService: VisitService, ctx: ExecutionContext) = {
    Future {   
      def top(t: ItemType) = response.topReferenced.map(_.count(t)).getOrElse(0)
      Visit.Search(args.query, Visit.Response(
          response.total, top(ItemType.PLACE), top(ItemType.PERSON)))         
    } flatMap { search =>
      logVisit(Some(response.took), Some(search))
    }
  }
  
}

object HasVisitLogging {
  
  // If one of these keywords appears in the UA header, treat as bot
  private val USER_AGENT_EXCLUDES = Set("uptimerobot")
   
  def isBot(visit: Visit) =
    USER_AGENT_EXCLUDES.find(visit.client.userAgent.contains(_)).isDefined
  
}