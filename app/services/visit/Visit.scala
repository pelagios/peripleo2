package services.visit

import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import services.HasDate
import services.item.PathHierarchy

case class Visit(    
  url       : String,
  referer   : Option[String], 
  visitedAt : DateTime,
  client    : Visit.Client,
  tookMs    : Option[Long],
  search    : Option[Visit.Search],
  selected  : Option[Visit.Selected])

object Visit extends HasDate {

  case class Client(
    ip        : String,    
    userAgent : String,
    browser   : String,
    os        : String,
    deviceType: String)

  case class Search(query: Option[String], response: Response)

  case class Response(totalHits: Long, topPlaces: Int, topPeople: Int)
  
  case class Selected(identifier: String, title: String, isInDataset: PathHierarchy)

  implicit val clientFormat: Format[Client] = (
    (JsPath \ "ip").format[String] and
    (JsPath \ "user_agent").format[String] and
    (JsPath \ "browser").format[String] and
    (JsPath \ "os").format[String] and
    (JsPath \ "device_type").format[String]
  )(Client.apply, unlift(Client.unapply))
      
  implicit val responseFormat: Format[Response] = (
    (JsPath \ "total_hits").format[Long] and
    (JsPath \ "top_places").format[Int] and
    (JsPath \ "top_people").format[Int]
  )(Response.apply, unlift(Response.unapply))
              
  implicit val searchFormat: Format[Search] = (
    (JsPath \ "query").formatNullable[String] and
    (JsPath \ "response").format[Response]
  )(Search.apply, unlift(Search.unapply))
  
  implicit val selectedFormat: Format[Selected] = (
    (JsPath \ "identifier").format[String] and
    (JsPath \ "title").format[String] and
    (JsPath \ "is_in_dataset").format[PathHierarchy]
  )(Selected.apply, unlift(Selected.unapply))

  implicit val visitFormat: Format[Visit] = (
    (JsPath \ "url").format[String] and
    (JsPath \ "referer").formatNullable[String] and
    (JsPath \ "visited_at").format[DateTime] and
    (JsPath \ "client").format[Client] and
    (JsPath \ "took_ms").formatNullable[Long] and
    (JsPath \ "search").formatNullable[Search] and
    (JsPath \ "selected").formatNullable[Selected]
  )(Visit.apply, unlift(Visit.unapply))

}




