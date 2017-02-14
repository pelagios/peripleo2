package services.item

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class PathHierarchy(path: Seq[String])

object PathHierarchy {
  
  implicit val pathHierarchyFormat: Format[PathHierarchy] =
    Format(
      JsPath.read[JsArray].map(paths => PathHierarchy(paths.value.map(_.as[JsString].value))),
      Writes[PathHierarchy](hierarchy => Json.toJson(hierarchy.path))
    )
  
}
