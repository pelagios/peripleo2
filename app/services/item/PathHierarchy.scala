package services.item

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class PathHierarchy(path: Seq[(String, String)])

object PathHierarchy {
  
  // Single beep to separate ID and title within each path segment when serialized
  val INNER_SEPARATOR = 0x0007.toChar
  
  // Double beep to separate the path segments when serialized
  val OUTER_SEPARATOR = Seq.fill(2)(INNER_SEPARATOR).mkString
  
  // For convenience, when the "hierarchy" is just one level  
  def apply(id: String, title: String) = new PathHierarchy(Seq((id, title)))
  
  def toHierarchy(levels: Seq[String]): PathHierarchy = 
    toHierarchies(levels).head
  
  def toOptHierarchy(maybeLevels: Option[Seq[String]]): Option[PathHierarchy] = 
    maybeLevels.map(toHierarchies(_).head)
    
  def fromHierarchy(hierarchy: PathHierarchy): Seq[String] =
    toList(hierarchy)

  def fromOptHierarchy(hierarchy: Option[PathHierarchy]): Option[Seq[String]] =
    hierarchy.map(toList)
    
  def fromHierarchies(hierarchies: Seq[PathHierarchy]): Seq[String] =
    hierarchies.flatMap(toList)
  
  /** Builds the 'levels' for the path. Example: the path
    * 
    *    [ "root", "middle", "leaf" ]
    * 
    * is translated to the levels
    * 
    *    [ "root", "root{SEPARATOR}middle", "root{SEPARATOR}middle{SEPARATOR}leaf" ] 
    */
  private def toList(hierarchy: PathHierarchy): Seq[String] =
    hierarchy.path.zipWithIndex.map { case (_, idx) =>
      hierarchy.path.take(idx + 1)
        .map(t => t._1 + INNER_SEPARATOR + t._2)
        .mkString(OUTER_SEPARATOR) }
  
  /** Rebuilds the path from a list of levels **/
  def toHierarchies(levels: Seq[String]): Seq[PathHierarchy] = {
    // Find the root paths (i.e. those that don't contain a double-beep separator)
    val roots = levels.filterNot(_.contains(OUTER_SEPARATOR))
    roots.map { root =>
      // For each root path, find the leaf path (i.e. the longest one starting with this root)
      val thisPath = levels.filter(_.startsWith(root)).maxBy(_.size)
      PathHierarchy(thisPath.split(OUTER_SEPARATOR).map { tuple =>
        val idAndTitle = tuple.split(INNER_SEPARATOR)
        (idAndTitle(0), idAndTitle(1)) 
      })
    }
  }

}
