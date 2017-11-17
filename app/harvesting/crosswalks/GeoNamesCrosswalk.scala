package harvesting.crosswalks

import com.vividsolutions.jts.geom.Coordinate
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import services.HasGeometry
import services.item._

object GeoNamesCrosswalk extends BaseGeoJSONCrosswalk {

  def fromJson(dataset: PathHierarchy)(record: String): Option[ItemRecord] = super.fromJson[GeoNamesRecord](record, { geonames =>
    ItemRecord(
      ItemRecord.normalizeURI(geonames.uri),
      Seq(ItemRecord.normalizeURI(geonames.uri)),
      DateTime.now(),
      None, // lastChangedAt
      geonames.title,
      Some(dataset),
      None, // isPartOf
      Seq.empty[Category],
      geonames.description.map(d => Seq(new Description(d))).getOrElse(Seq.empty[Description]),
      None, // homepage
      None, // license
      geonames.names.flatMap(_.language),
      Seq.empty[Depiction],
      geonames.features.headOption.map(_.geometry), // TODO compute union?
      geonames.representativePoint,
      None, // temporalBounds
      geonames.names,
      Seq.empty[Link],
      None, None)
  })

}

case class GeoNamesRecord(
  uri                 : String,
  title               : String,
  description         : Option[String],
  names               : Seq[Name],
  features            : Seq[Feature],
  representativePoint : Option[Coordinate],
  countryCode         : Option[String],
  population          : Option[Long])

object GeoNamesRecord extends HasGeometry {

  implicit val geonamesRecordReads: Reads[GeoNamesRecord] = (
    (JsPath \ "uri").read[String] and
    (JsPath \ "title").read[String] and
    (JsPath \ "description").readNullable[String] and
    (JsPath \ "names").readNullable[Seq[Name]].map(_.getOrElse(Seq.empty[Name])) and
    (JsPath \ "features").readNullable[Seq[Feature]].map(_.getOrElse(Seq.empty[Feature])) and
    (JsPath \ "reprPoint").readNullable[Coordinate] and
    (JsPath \ "country_code").readNullable[String] and
    (JsPath \ "population").readNullable[Long]
  )(GeoNamesRecord.apply _)

}
