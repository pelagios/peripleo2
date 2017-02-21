package services.item.search

import scala.util.Try

case class SearchArgs(
    
  query: Option[String],
  
  limit: Int,
  
  offset: Int)
  
object SearchArgs {
  
 private def getArg(key: String, queryString: Map[String, Seq[String]]): Option[String] = 
   queryString
      .filter(_._1.equalsIgnoreCase(key))
      .headOption.flatMap(_._2.headOption)
  
  def fromQueryString(q: Map[String, Seq[String]]) = SearchArgs(
    getArg("q", q),
    getArg("limit", q).map(_.toInt).getOrElse(20),
    getArg("offset", q).map(_.toInt).getOrElse(0)
  )

}