package services.item.search

import services.item.place.Place

case class TopPlaces private(places: Seq[(Place, Long)])

object TopPlaces {
  
  def build(counts: Seq[(String, Long)], places: Seq[Place]): TopPlaces =
    TopPlaces(counts.map { case (uri, count) => (places.find(_.rootUri == uri).get, count) })
  
}