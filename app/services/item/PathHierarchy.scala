package services.item

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class PathSegment(id: String, title: String) {
  
  override def toString() = id + PathSegment.SEPARATOR + title
    
}

object PathSegment {
  
  val SEPARATOR = 0x0007.toChar
  
}

case class PathHierarchy(path: Seq[PathSegment])
    
object PathHierarchy {
  
  // We're using single Beeps to separate id and title within each segment,
  // and then a double Beep to separate the segments
  val SEPARATOR = Seq(PathSegment.SEPARATOR, PathSegment.SEPARATOR).mkString
  
  def toHierarchy(maybeLevels: Option[Seq[String]]): Option[PathHierarchy] = 
    maybeLevels.map(toPathHierarchies(_).head)
      
  def toHierarchies(maybeLevels: Option[Seq[String]]): Seq[PathHierarchy] =
    maybeLevels.map(toPathHierarchies).getOrElse(Seq.empty[PathHierarchy])

  def fromHierarchy(hierarchy: Option[PathHierarchy]): Option[Seq[String]] =
    hierarchy.map(toList)

  def fromHierarchies(hierarchies: Seq[PathHierarchy]): Option[Seq[String]] =
    if (hierarchies.isEmpty) None
    else Some(hierarchies.flatMap(toList))
  
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
      hierarchy.path.take(idx + 1).mkString(SEPARATOR) }
  
  /** Rebuilds the path from a list of levels **/
  private def toPathHierarchies(levels: Seq[String]): Seq[PathHierarchy] = {
    // Find the root paths (i.e. those that don't contain a double-beep separator)
    val roots = levels.filterNot(_.contains(SEPARATOR))

    roots.map { root =>
      // For each root path, find the leaf path (i.e. the longest one starting with this root)
      val thisPath = levels.filter(_.startsWith(root)).maxBy(_.size)
      PathHierarchy(thisPath.split(SEPARATOR).map { tuple =>
        val idAndTitle = tuple.split(PathSegment.SEPARATOR)
        PathSegment(idAndTitle(0), idAndTitle(1)) 
      })
    }
  }

}
