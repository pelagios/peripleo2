package harvesting.crosswalks

import com.vividsolutions.jts.geom.Geometry
import java.io.InputStream
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import services.{ HasGeometry, HasNullableSeq }
import services.item._
import play.api.Logger

object FeatureCollectionCrosswalk {
  
   def fromGeoJSON(dataset: PathHierarchy): InputStream => Seq[ItemRecord] = { in =>
     
     def toItemRecord(json: JsValue): Option[ItemRecord] = {
       try {
         val f = Json.fromJson[GazetteerFeature](json).get
         Some(ItemRecord(
           f.uri,
           Seq(f.uri),
           DateTime.now(),
           None, // lastChangedAt
           f.title,
           Some(dataset),
           None, // isPartOf
           Seq.empty[Category], // TODO get from properties > feature types?
           Seq.empty[Description],
           None, // homepage
           None, // license
           Seq.empty[Language],
           Seq.empty[Depiction],
           f.geometry,
           f.geometry.map(_.getCentroid.getCoordinate),
           None, // Temporal bounds TODO support when
           f.names,
           None,
           f.links.map(_.closeMatches).getOrElse(Seq.empty[String]),
           f.links.map(_.exactMatches).getOrElse(Seq.empty[String])
         ))
       } catch { case t: Throwable =>
         t.printStackTrace()
         Logger.warn("Discarding broken place record")
         Logger.warn(json.toString)
         None
       }
     }
     
     (Json.parse(in) \ "features") match {
       case JsDefined(JsArray(values)) => values.flatMap(toItemRecord)
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
  
object Links extends HasNullableSeq {
  
  implicit val linkReads: Reads[Links] = (
    (JsPath \ "exact_matches").readNullable[Seq[String]].map(fromOptSeq[String]) and
    (JsPath \ "close_matches").readNullable[Seq[String]].map(fromOptSeq[String])
  )(Links.apply _)
  
}

object GazetteerFeature extends HasGeometry with HasNullableSeq {
  
  implicit val gazetteerFeatureReads: Reads[GazetteerFeature] = (
    (JsPath \ "uri").read[String] and
    (JsPath \ "title").read[String] and
    (JsPath \ "geometry").readNullable[Geometry] and
    (JsPath \ "names").readNullable[Seq[Name]].map(fromOptSeq[Name]) and
    (JsPath \ "links").readNullable[Links]
  )(GazetteerFeature.apply _)
}

