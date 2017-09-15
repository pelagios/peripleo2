package harvesting.crosswalks

import java.io.InputStream
import services.item.ItemRecord
import play.api.libs.json._
import services.item.Name
import com.vividsolutions.jts.geom.Geometry

object FeatureCollectionCrosswalk {
  
   def fromGeoJSON(filename: String): InputStream => Seq[ItemRecord] = { in =>
     
     def toItemRecord(json: JsValue): ItemRecord = ???
     
     (Json.parse(in) \ "features") match {
       case JsDefined(JsArray(values)) => values.map(toItemRecord)
       case _ => throw new Exception("Invalid FeatureCollection")
     }
   }
  
}

case class Links(exactMatches: Seq[String], closeMatches: Seq[String])

case class GazetteerFeature(
  uri      : String,
  title    : String, 
  geometry : Option[Geometry],
  names    : Seq[Name],
  links    : Option[Links])
  
object Links {
  
}

object GazetteerFeature {
  
}

