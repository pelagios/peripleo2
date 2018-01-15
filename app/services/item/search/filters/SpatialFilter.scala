package services.item.search.filters

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.searches.queries.QueryDefinition
import com.vividsolutions.jts.geom.Coordinate
import es.ES
import org.apache.lucene.search.join.ScoreMode
import org.elasticsearch.common.geo.ShapeRelation
import org.elasticsearch.common.geo.builders.ShapeBuilders
import org.elasticsearch.common.unit.DistanceUnit
import org.elasticsearch.index.query.{QueryBuilders, GeoShapeQueryBuilder}

case class GeoShapeQueryDefinition(builder: GeoShapeQueryBuilder) extends QueryDefinition

case class SpatialFilter(bbox: Option[BoundingBox], center: Option[Coordinate], radius: Option[Double]) {
  
  require(bbox.isDefined || center.isDefined)
  
  private lazy val shape = (bbox, center) match {
    case (Some(b), _) =>
      ShapeBuilders.newEnvelope(
        new Coordinate(b.minLon, b.maxLat),
        new Coordinate(b.maxLon, b.minLat))
      
    case (_, Some(c)) =>
      ShapeBuilders.newCircleBuilder()
        .center(c)
        .radius(radius.getOrElse(1.0), DistanceUnit.KILOMETERS)
        
    case _ =>
      // Can never happen due to require(...), but just to avoid compiler warning
      throw new RuntimeException   
  }
  
  private def buildQueryDefinition(field: String) =
    new GeoShapeQueryDefinition(QueryBuilders.geoShapeQuery(field, shape).relation(ShapeRelation.WITHIN))
  
  lazy val filterDefinition = 
    boolQuery should (
      buildQueryDefinition("bbox"),
      hasChildQuery(ES.REFERENCE) query { buildQueryDefinition("reference_to.bbox") } scoreMode ScoreMode.Avg
    )

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