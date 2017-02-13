package services.search

import com.vividsolutions.jts.geom.{ Coordinate, Geometry }
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import services.HasGeometry

case class Reference(
    
  referenceType: ReferenceType.Value,
  
  relation: Option[Relation.Value],
  
  uri: Option[String],
  
  geometry: Option[Geometry],
  
  representativePoint: Option[Coordinate],
  
  context: Option[String]
    
)

object Reference extends HasGeometry {
  
  implicit val referenceFormat: Format[Reference] = (
    (JsPath \ "reference_type").format[ReferenceType.Value] and
    (JsPath \ "relation").formatNullable[Relation.Value] and
    (JsPath \ "uri").formatNullable[String] and
    (JsPath \ "geometry").formatNullable[Geometry] and
    (JsPath \ "representative_point").formatNullable[Coordinate] and
    (JsPath \ "context").formatNullable[String]
  )(Reference.apply, unlift(Reference.unapply))
  
}

