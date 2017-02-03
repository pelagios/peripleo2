package services.search

import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import com.vividsolutions.jts.geom.{ Coordinate, Geometry }

case class Record(
    
  identifiers: Seq[String],
  
  recordType: RecordType.Value,
  
  lastSyncedAt: DateTime,

  lastChangedAt: Option[DateTime],
  
  categories: Seq[Category],
  
  title: String,
  
  // isInDataset
  
  // isPartOf
  
  descriptions: Seq[Description],
  
  homepage: Option[String],
  
  languages: Seq[Language],
  
  geometry: Option[Geometry],
  
  representativePoint: Option[Coordinate],
  
  temporalBounds: Option[TemporalBounds],
  
  places: Seq[String],

  people: Seq[String],
  
  periods: Seq[String],
  
  depictions: Seq[Depiction]
  
)
