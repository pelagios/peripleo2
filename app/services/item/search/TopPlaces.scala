package services.item.search

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import services.item.Item

case class TopPlaces private(places: Seq[(Item, Long)])

object TopPlaces {
  
  def build(counts: Seq[(String, Long)], places: Seq[Item]): TopPlaces =
    TopPlaces(counts.map { case (docId, count) => (places.find(_.docId.toString == docId).get, count) })
    
  implicit val topPlacesWrites = 
    Writes[TopPlaces](p => Json.toJson(p.places.map { case (place, count) => 
      Json.toJson(place).as[JsObject] ++ Json.obj("result_count" -> count) }))
    
}