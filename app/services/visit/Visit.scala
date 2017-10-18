package services.visit

import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.HasDate
import services.visit.info._

case class Visit(    
  url          : String,
  referer      : Option[String], 
  visitedAt    : DateTime,
  visitType    : VisitType.Value,
  client       : Client,
  responseTime : Option[Long],
  search       : Option[SearchInfo],
  selection    : Option[SelectionInfo]
)

object Visit extends HasDate {
  
  implicit val visitFormat: Format[Visit] = (
    (JsPath \ "url").format[String] and
    (JsPath \ "referer").formatNullable[String] and
    (JsPath \ "visited_at").format[DateTime] and
    (JsPath \ "visit_type").format[VisitType.Value] and
    (JsPath \ "client").format[Client] and
    (JsPath \ "response_time").formatNullable[Long] and
    (JsPath \ "search").formatNullable[SearchInfo] and
    (JsPath \ "selection").formatNullable[SelectionInfo]
  )(Visit.apply, unlift(Visit.unapply))

}




