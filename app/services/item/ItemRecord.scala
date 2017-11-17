package services.item

import com.vividsolutions.jts.geom.{ Coordinate, Geometry }
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.{ HasDate, HasGeometry, HasNullableSeq }

case class ItemRecord(
  uri                 : String,
  identifiers         : Seq[String],
  lastSyncedAt        : DateTime,
  lastChangedAt       : Option[DateTime],
  title               : String,
  isInDataset         : Option[PathHierarchy],
  isPartOf            : Option[PathHierarchy],
  categories          : Seq[Category],
  descriptions        : Seq[Description],
  homepage            : Option[String],
  license             : Option[String],
  languages           : Seq[Language],
  depictions          : Seq[Depiction],
  geometry            : Option[Geometry],
  representativePoint : Option[Coordinate],
  temporalBounds      : Option[TemporalBounds],
  names               : Seq[Name],
  links               : Seq[Link],
  publisher           : Option[String],
  harvestVia          : Option[String]
) {

  // For convenience: all close and exact match URIs
  lazy val directMatches = 
    links.filter(l => l.linkType == LinkType.CLOSE_MATCH || l.linkType == LinkType.EXACT_MATCH)
      .map(_.uri)
      
  def getLinks(t: LinkType.Value) =
    links.filter(_.linkType == t)

  /** Returns true if there is a connection between the two records.
    *
    * A connection can result from one of the following three causes:
    * - this record lists the other record's URI as a close- or exactMatch
    * - the other record lists this record's URI as a close- or exactMatch
    * - both records share at least one close/exactMatch URI
    */
  def isConnectedWith(other: ItemRecord): Boolean = {
    val thisIdentifiers = (identifiers ++ directMatches).toSet
    val otherIdentifiers = (other.identifiers ++ other.directMatches).toSet
    thisIdentifiers.intersect(otherIdentifiers).size > 0
  }

}

object ItemRecord extends HasDate with HasNullableSeq with HasGeometry {
  
  /** Utility method to normalize a URI to a standard format.
    *
    * The following changes are applied to URIs:
    * - remove '#this' suffixes as used by Pleiades
    * - removes trailing slashes
    * - changes https:// URIs to http://
    */
  def normalizeURI(uri: String) = {
    val noThis = if (uri.indexOf("#this") > -1) uri.substring(0, uri.indexOf("#this")) else uri
    val httpOnly = noThis.replace("https://", "http://")
    if (httpOnly.endsWith("/"))
      httpOnly.substring(0, httpOnly.size - 1)
    else
      httpOnly
  }

  /** Utility to create a cloned record, with all URIs normalized **/
  def normalize(i: ItemRecord) =
    i.copy(
      uri = normalizeURI(i.uri),
      identifiers = i.identifiers.map(normalizeURI),
      isInDataset = i.isInDataset.map(_.normalize),
      isPartOf = i.isPartOf.map(_.normalize),
      links = i.links.map(_.normalize)
    )

  implicit val itemRecordFormat: Format[ItemRecord] = (
    (JsPath \ "uri").format[String] and
    (JsPath \ "identifiers").format[Seq[String]] and
    (JsPath \ "last_synced_at").format[DateTime] and
    (JsPath \ "last_changed_at").formatNullable[DateTime] and
    (JsPath \ "title").format[String] and
    (JsPath \ "is_in_dataset").formatNullable[PathHierarchy] and
    (JsPath \ "is_part_of").formatNullable[PathHierarchy] and
    (JsPath \ "categories").formatNullable[Seq[Category]]
      .inmap[Seq[Category]](fromOptSeq[Category], toOptSeq[Category]) and
    (JsPath \ "descriptions").formatNullable[Seq[Description]]
      .inmap[Seq[Description]](fromOptSeq[Description], toOptSeq[Description]) and
    (JsPath \ "homepage").formatNullable[String] and
    (JsPath \ "license").formatNullable[String] and
    (JsPath \ "languages").formatNullable[Seq[Language]]
      .inmap[Seq[Language]](fromOptSeq[Language], toOptSeq[Language]) and
    (JsPath \ "depictions").formatNullable[Seq[Depiction]]
      .inmap[Seq[Depiction]](fromOptSeq[Depiction], toOptSeq[Depiction]) and
    (JsPath \ "geometry").formatNullable[Geometry] and
    (JsPath \ "representative_point").formatNullable[Coordinate] and
    (JsPath \ "temporal_bounds").formatNullable[TemporalBounds] and
    (JsPath \ "names").formatNullable[Seq[Name]]
      .inmap[Seq[Name]](fromOptSeq[Name], toOptSeq[Name]) and
    (JsPath \ "links").formatNullable[Seq[Link]]
      .inmap[Seq[Link]](fromOptSeq[Link], toOptSeq[Link]) and
    (JsPath \ "publisher").formatNullable[String] and
    (JsPath \ "harvest_url").formatNullable[String]
  )(ItemRecord.apply, unlift(ItemRecord.unapply))

}
