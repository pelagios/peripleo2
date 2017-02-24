package services.item.search

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import services.item.place.Place

case class TopPlaces private(places: Seq[(Place, Long)])

object TopPlaces {
  
  def build(counts: Seq[(String, Long)], places: Seq[Place]): TopPlaces =
    TopPlaces(counts.map { case (uri, count) => (places.find(_.rootUri == uri).get, count) })
    
  implicit val topPlacesWrites = 
    Writes[TopPlaces](p => Json.toJson(p.places.map { case (place, count) => 
      Json.toJson(place).as[JsObject] ++ Json.obj("result_count" -> count) }))
    
}