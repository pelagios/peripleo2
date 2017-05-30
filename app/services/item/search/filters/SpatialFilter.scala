package services.item.search.filters

import com.sksamuel.elastic4s.ElasticDsl._
import com.vividsolutions.jts.geom.Coordinate
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.common.geo.builders.ShapeBuilder
import org.elasticsearch.index.query.GeoShapeQueryBuilder
import com.sksamuel.elastic4s.QueryDefinition
import org.elasticsearch.common.geo.ShapeRelation
import org.elasticsearch.common.unit.DistanceUnit

case class GeoShapeDefinition(builder: GeoShapeQueryBuilder) extends QueryDefinition

case class SpatialFilter(bbox: Option[BoundingBox], center: Option[Coordinate], radius: Option[Double]) {
  
  require(bbox.isDefined || center.isDefined)
  
  def filterDefinition(field: String) = (bbox, center) match {
    case (Some(b), _) =>
      val builder = 
        QueryBuilders.geoShapeQuery(field, ShapeBuilder.newEnvelope()
          .topLeft(b.minLon, b.maxLat)
          .bottomRight(b.maxLon, b.minLat)
        ).relation(ShapeRelation.WITHIN)
          
      new GeoShapeDefinition(builder)
      
    case (_, Some(c)) =>
      val builder =
        QueryBuilders.geoShapeQuery(field, ShapeBuilder.newCircleBuilder()
          .center(c)
          .radius(radius.getOrElse(1.0), DistanceUnit.KILOMETERS)
        ).relation(ShapeRelation.WITHIN)
          
      new GeoShapeDefinition(builder)
       
    case _ =>
      // Can never happen due to require(...), but just to avoid compiler warning
      throw new RuntimeException    
  }
  
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