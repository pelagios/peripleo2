package controllers.api.legacy.response

import services.item.TemporalBounds
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class LegacyTemporalBounds(start: Int, end: Int)

object LegacyTemporalBounds {
  
  def fromBounds(b: TemporalBounds) = {
    LegacyTemporalBounds(b.from.getYear, b.to.getYear)    
  }
  
  implicit val legacyTemporalBoundsWrites: Writes[LegacyTemporalBounds] = (
    (JsPath \ "start").write[Int] and
    (JsPath \ "end").write[Int]
  )(unlift(LegacyTemporalBounds.unapply))
  
}
