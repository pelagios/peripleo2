package services.item

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class PathHierarchy(path: Seq[(String, String)]) {
  
  lazy val normalize =    
    PathHierarchy(path.map { case (id, label) =>
      val normalizedId = if (id.startsWith("http"))
        // In case the ID is a URI, remove trailing slashes (Peripleo-wide convention)
        if (id.endsWith("/")) id.substring(0, id.size - 1) else id
      else
        id 

      (normalizedId, label)
    })  
    
  private lazy val paths: Seq[String] = 
    path.zipWithIndex.map { case (_, idx) =>
      path.take(idx + 1)
        .map(t => t._1 + PathHierarchy.INNER_SEPARATOR + t._2)
        .mkString(PathHierarchy.OUTER_SEPARATOR) }
  
  private lazy val root = {
    val r = path.head
    r._1 + PathHierarchy.INNER_SEPARATOR + r._2
  }
  
  lazy val ids: Seq[String] = path.map(_._1)
  
  def append(id: String, label: String): PathHierarchy =
    PathHierarchy(path :+ (id, label))
  
}

object PathHierarchy {
  
  // Single beep to separate ID and title within each path segment when serialized
  val INNER_SEPARATOR = 0x0007.toChar
  
  // Double beep to separate the path segments when serialized
  val OUTER_SEPARATOR = Seq.fill(2)(INNER_SEPARATOR).mkString
  
  // For convenience, when the "hierarchy" is just one level  
  def apply(id: String, title: String) = new PathHierarchy(Seq((id, title)))
  
  /** Rebuilds the path from a list of levels **/
  def parse(serialized: Seq[String]): PathHierarchy = {
    // Find the (first) root path (i.e. the one that doesn't contain a double-beep separator)
    val root = serialized.filterNot(_.contains(OUTER_SEPARATOR)).head 
    
    // Find the leaf path (i.e. the longest one starting with the root)
    val leafPath = serialized.filter(_.startsWith(root)).maxBy(_.size)
    PathHierarchy(leafPath.split(OUTER_SEPARATOR).map { tuple =>
      val idAndTitle = tuple.split(INNER_SEPARATOR)
      (idAndTitle(0), idAndTitle(1)) 
    })
  }
  
  implicit val pathHierarchyReads: Reads[PathHierarchy] =
    (JsPath \ "paths").read[Seq[String]].map(serialized => PathHierarchy.parse(serialized))
  
  implicit val pathHierarchyWrites: Writes[PathHierarchy] = (
    (JsPath \ "root").write[String] and
    (JsPath \ "paths").write[Seq[String]] and
    (JsPath \ "ids").write[Seq[String]]
  )(p => (
      p.root,
      p.paths, 
      p.ids
  ))

}
