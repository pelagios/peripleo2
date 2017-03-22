package services.item

import play.api.libs.json.{ Json, JsPath, Writes }
import play.api.libs.functional.syntax._

case class ReferenceStats(identifier: String, aggregation: Map[ReferenceType.Value, (Long, Map[String, Long])]) {
  
  def byType: Map[ReferenceType.Value, Long] =
    aggregation.map { case (key, (count, _)) => (key, count) }
  
  def getForType(t: ReferenceType.Value): Map[String, Long] =
    aggregation.get(t).map(_._2).getOrElse(Map.empty[String, Long])    
  
}

object ReferenceStats {
  
  implicit val statsTupleWrites = Writes[(String, Long)] { tuple =>
    Json.obj("uri" -> tuple._1, "count" -> tuple._2)
  }
  
  implicit val placeWrites: Writes[ReferenceStats] = (
    (JsPath \ "identifier").write[String] and
    (JsPath \ "references").write[Map[String, Seq[(String, Long)]]]
  )(stats => (
      stats.identifier,
      stats.aggregation.map { t => (t._1.toString, t._2._2.toSeq) }
  ))
  
}