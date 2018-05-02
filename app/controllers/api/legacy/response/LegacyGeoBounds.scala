package controllers.api.legacy.response

import play.api.libs.json._
import play.api.libs.functional.syntax._
import com.vividsolutions.jts.geom.Envelope

case class LegacyGeoBounds(minLon: Double, maxLon: Double, minLat: Double, maxLat: Double)

object LegacyGeoBounds {
  
  def fromEnvelope(e: Envelope) =
    LegacyGeoBounds(
      e.getMinX,
      e.getMaxX,
      e.getMinY,
      e.getMaxY)
  
  implicit val legacyGeoBoundsWrites: Writes[LegacyGeoBounds] = (
    (JsPath \ "min_lon").write[Double] and
    (JsPath \ "max_lon").write[Double] and
    (JsPath \ "min_lat").write[Double] and
    (JsPath \ "max_lat").write[Double]
  )(unlift(LegacyGeoBounds.unapply))
  
}