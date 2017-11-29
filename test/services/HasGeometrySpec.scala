package services

import com.vividsolutions.jts.geom.{Coordinate, Geometry, GeometryFactory}
import org.scalatestplus.play._
import play.api.libs.json._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json.Json
import helpers.TestHelpers

class HasGeometrySpec extends PlaySpec with TestHelpers with HasGeometry {
 
  private val factory = 
 
  "HasGeometry" should {
   
    "parse MultiPoint geometry" in {
      val expected = new GeometryFactory().createMultiPoint(Array(
        new Coordinate(15.412889, 30.578145),
        new Coordinate(15.4027724, 30.5764395),
        new Coordinate(15.39579, 30.58165),
        new Coordinate(15.406692, 30.596161)
      ))

      val json = Json.parse(loadTextfile("services/multipoint_geometry.json"))
      val geom = parseSafe((json \\ "geometry").head)
      
      geom.isSuccess mustBe true
      geom.get mustBe expected
    }
    
  }
  
}