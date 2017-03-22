package services.item

import play.api.libs.json.{ Json, JsPath, Writes }
import play.api.libs.functional.syntax._
import services.item.place.Place

/** TODO this is currently a horrible hack and needs to change once we introduce more than just place references **/
case class ReferenceStats(identifier: String, aggregation: Map[ReferenceType.Value, (Long, Map[Place, Long])]) {
  
  lazy val byType: Map[ReferenceType.Value, Long] =
    aggregation.map { case (key, (count, _)) => (key, count) }
  
  def getForType(t: ReferenceType.Value): Map[Place, Long] =
    aggregation.get(t).map(_._2).getOrElse(Map.empty[Place, Long])   
  
}

object ReferenceStats {
  
  // TODO hack
  def build(identifier: String, aggregation: Map[ReferenceType.Value, (Long, Map[String, Long])], places: Set[Place]) = {
    val resolved = aggregation.map { case (referenceType, (totalCount, subaggregation)) =>
      (referenceType, (totalCount, subaggregation.map { case (uri, count) =>
        val place = places.find(_.rootUri == uri).get
        (place, count) 
      }))
    }
    
    ReferenceStats(identifier, resolved)
  }
  
  implicit val statsTupleWrites: Writes[(Place, Long)] = (
    (JsPath \ "uri").write[String] and
    (JsPath \ "title").write[String] and
    (JsPath \ "description").writeNullable[String] and
    (JsPath \ "count").write[Long]
  )(tuple => (
      tuple._1.rootUri,
      tuple._1.titles.head,
      tuple._1.descriptions.headOption.map(_._1.description),
      tuple._2
  ))
  
  implicit val placeWrites: Writes[ReferenceStats] = (
    (JsPath \ "identifier").write[String] and
    (JsPath \ "references").write[Map[String, Seq[(Place, Long)]]]
  )(stats => (
      stats.identifier,
      stats.aggregation.map { t => (t._1.toString, t._2._2.toSeq) }
  ))
  
}