package services.item.search.filters

import com.vividsolutions.jts.geom.Coordinate

case class SpatialFilter(bbox: Option[BoundingBox], center: Option[Coordinate], radius: Option[Double]) {
  
  require(bbox.isDefined || center.isDefined)
  
}

object SpatialFilter {
  
  val DEFAULT_RADIUS = 1.0
  
}

case class BoundingBox(minLon: Double, maxLon: Double, minLat: Double, maxLat: Double) {
  
  require(minLon >= -180 && minLon <= 180)
  require(maxLon >= -180 && minLon <= 180)
  require(minLat >=  -90 && minLat <=  90)
  require(maxLat >=  -90 && maxLat <=  90)
  require(minLon <=  maxLon)
  require(minLat <=  maxLat)
  
}

object BoundingBox {
  
  def fromString(s: String): BoundingBox = {
    val coords = s.split(",").map(_.trim)
    BoundingBox(coords(0).toDouble, coords(1).toDouble, coords(2).toDouble, coords(3).toDouble)
  }
  
}