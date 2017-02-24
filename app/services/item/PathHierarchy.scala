package services.item

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class PathHierarchy(path: Seq[String])

object PathHierarchy {
  
  private val SEPARATOR = 0x0007.toChar // Beep

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
      hierarchy.path.take(idx + 1).mkString(SEPARATOR.toString) }
  
  private def toPathHierarchies(levels: Seq[String]): Seq[PathHierarchy] = {
    val roots = levels.filterNot(_.contains(SEPARATOR))
    
    roots.map { root =>
      val thisPath = levels.filter(_.startsWith(root)).maxBy(_.size)
      PathHierarchy(thisPath.split(SEPARATOR)) }
  }

}
