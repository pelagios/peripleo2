package controllers.api.legacy.response

import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.item.PathHierarchy

case class LegacyPathSegment(title: String, id: String)

object LegacyPathSegment {

  implicit val legacyPathSegmentWrites: Writes[LegacyPathSegment] = (
    (JsPath \ "title").write[String] and
    (JsPath \ "id").write[String]
  )(unlift(LegacyPathSegment.unapply))
  
}

object LegacyDatasetPath {
  
  def fromHierarchy(h: PathHierarchy) =
    h.path.map(t => LegacyPathSegment(t._2, t._1))
  
}