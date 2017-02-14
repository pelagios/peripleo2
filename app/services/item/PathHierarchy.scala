package services.item

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class PathHierarchy(path: Seq[String])

object PathHierarchy {
  
  private val SEPARATOR = 0x0007.toChar // Beep
  
  implicit val pathHierarchyFormat: Format[PathHierarchy] =
    Format(
      JsPath.read[JsArray].map { list => 
        val longestPath = list.value // Just take longest and split by separator
          .map(_.as[JsString].value)
          .maxBy(_.size)
    
        PathHierarchy(longestPath.split(SEPARATOR)) 
      },
      
      Writes[PathHierarchy] { hierarchy => 
        val paths = hierarchy.path.zipWithIndex.map { case (_, idx) =>
          hierarchy.path.take(idx + 1).mkString(SEPARATOR.toString) }
        
        Json.toJson(paths) 
      }
    )
  
}
