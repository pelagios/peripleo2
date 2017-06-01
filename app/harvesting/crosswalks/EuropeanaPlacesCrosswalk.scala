package harvesting.crosswalks

import com.vividsolutions.jts.geom.{ Coordinate, GeometryFactory }
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import services.HasGeometry
import services.item._

/** Just a quick hack.
  * 
  * TODO develop a common solution for a generic GeoJSON-based gazetteer.
  */
object EuropeanaPlacesCrosswalk extends BaseGeoJSONCrosswalk {
  
  val factory = new GeometryFactory()

  def fromJson(record: String): Option[ItemRecord] = super.fromJson[EuropeanaPlaceRecord](record, { europeana =>
    ItemRecord(
      ItemRecord.normalizeURI(europeana.uri),
      Seq(ItemRecord.normalizeURI(europeana.uri)),
      DateTime.now(),
      None, // lastChangedAt
      europeana.title,
      Some(PathHierarchy("Europeana", "Europeana")),
      None, // isPartOf
      Seq.empty[Category],
      Seq.empty[Description],
      None, // homepage
      None, // license
      europeana.names.flatMap(_.language),
      Seq.empty[Depiction],
      europeana.representativePoint.map(pt => factory.createPoint(pt)),
      europeana.representativePoint,
      None, // temporalBounds
      europeana.names,
      europeana.closeMatches,
      Seq.empty[String]  // exactMatches
    )
  })

}

case class EuropeanaPlaceRecord(
  uri                 : String,
  title               : String,
  names               : Seq[Name],
  representativePoint : Option[Coordinate],
  closeMatches        : Seq[String])

object EuropeanaPlaceRecord extends HasGeometry {

  implicit val europeanaPlaceRecordReads: Reads[EuropeanaPlaceRecord] = (
    (JsPath \ "uri").read[String] and
    (JsPath \ "title").read[String] and
    (JsPath \ "names").read[Seq[Name]] and
    (JsPath \ "lonloat").readNullable[Coordinate] and
    (JsPath \ "close_matches").read[Seq[String]]
  )(EuropeanaPlaceRecord.apply _)

}
