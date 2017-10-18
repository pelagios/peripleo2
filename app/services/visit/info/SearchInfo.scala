package services.visit.info

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class SearchInfo(query: Option[String], response: SearchInfo.Returned)
  
object SearchInfo {
  
  case class Returned(totalHits: Long, topPlaces: Int, topPeople: Int, topPeriods: Int)
  
  object Returned {
    
    implicit val returnedFormat: Format[Returned] = (
      (JsPath \ "total_hits").format[Long] and
      (JsPath \ "top_places").format[Int] and
      (JsPath \ "top_people").format[Int] and
      (JsPath \ "top_periods").format[Int]
    )(Returned.apply, unlift(Returned.unapply))
  
  }
  
  implicit val searchInfoFormat: Format[SearchInfo] = (
    (JsPath \ "query").formatNullable[String] and
    (JsPath \ "returned").format[Returned]
  )(SearchInfo.apply, unlift(SearchInfo.unapply))
  
}