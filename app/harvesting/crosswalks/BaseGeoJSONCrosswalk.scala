package harvesting.crosswalks

import com.vividsolutions.jts.geom.Geometry
import play.api.Logger
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import services.HasGeometry
import services.item.ItemRecord

trait BaseGeoJSONCrosswalk {
  
  def fromJson[T](record: String, crosswalk: T => ItemRecord)(implicit reads: Reads[T]): Option[ItemRecord] =
    
    Json.fromJson[T](Json.parse(record)) match {
    
      case s: JsSuccess[T] => Some(crosswalk(s.get))
        
      case e: JsError =>
        Logger.error(e.toString)      
        None
        
    }
  
}

case class Feature(geometry: Geometry)

object Feature extends HasGeometry {
  
  implicit val featureReads: Reads[Feature] =
    (JsPath \ "geometry").read[Geometry].map(Feature(_))
  
}