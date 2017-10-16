package controllers

import eu.bitwalker.useragentutils.UserAgent
import org.joda.time.DateTime
import play.api.mvc.{ AnyContent, RequestHeader }
import play.api.http.HeaderNames
import scala.concurrent.Future
import services.visit._

trait HasVisitLogging {
    
  private def log()(implicit request: RequestHeader, visitService: VisitService): Future[Unit] = {
    val userAgentHeader = request.headers.get(HeaderNames.USER_AGENT)
    val userAgent = userAgentHeader.map(ua => UserAgent.parseUserAgentString(ua))
    val os = userAgent.map(_.getOperatingSystem)
    
    val visit = Visit(
      request.uri,
      request.headers.get(HeaderNames.REFERER),
      DateTime.now(),
      Client(
        request.remoteAddress,
        userAgentHeader.getOrElse("UNKNOWN"),
        userAgent.map(_.getBrowser.getGroup.getName).getOrElse("UNKNOWN"),
        os.map(_.getName).getOrElse("UNKNOWN"),
        os.map(_.getDeviceType.getName).getOrElse("UNKNOWN")  
      ))
    
    if (HasVisitLogging.isBot(visit))
      Future.successful(())
    else
      visitService.insertVisit(visit)    
  }
  
}

object HasVisitLogging {
  
  // If one of these keywords appears in the UA header, treat as bot
  private val USER_AGENT_EXCLUDES = Set("uptimerobot")
   
  def isBot(visit: Visit) =
    USER_AGENT_EXCLUDES.find(visit.client.userAgent.contains(_)).isDefined
  
}