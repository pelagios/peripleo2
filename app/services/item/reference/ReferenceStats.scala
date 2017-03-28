package services.item.reference

import play.api.libs.json.{ Json, JsPath, Writes }
import play.api.libs.functional.syntax._
import services.HasNullableSeq
import services.item.Item

case class ReferenceStats(aggregation: Map[ReferenceType.Value, (Long, Map[Item, Long])]) {

  lazy val byType: Map[ReferenceType.Value, Long] =
    aggregation.map { case (key, (count, _)) => (key, count) }

  def getForType(t: ReferenceType.Value): Map[Item, Long] =
    aggregation.get(t).map(_._2).getOrElse(Map.empty[Item, Long])

}

object ReferenceStats extends HasNullableSeq {

  def build(aggregation: Map[ReferenceType.Value, (Long, Map[String, Long])], resolvedItems: Set[Item]) = {
    val resolved = aggregation.map { case (referenceType, (totalCount, subaggregation)) =>
      (referenceType, (totalCount, subaggregation.map { case (docId, count) =>
        val item = resolvedItems.find(_.docId.toString == docId).get
        (item, count)
      }))
    }

    ReferenceStats(resolved)
  }

  implicit val statsTupleWrites: Writes[(Item, Long)] = (
    (JsPath \ "identifiers").write[Seq[String]] and
    (JsPath \ "title").write[String] and
    (JsPath \ "descriptions").formatNullable[Seq[String]]
      .inmap[Seq[String]](fromOptSeq[String], toOptSeq[String]) and
    (JsPath \ "count").write[Long]
  )(tuple => (
      tuple._1.identifiers,
      tuple._1.title,
      tuple._1.isConflationOf.flatMap(_.descriptions.map(_.description)),
      tuple._2
  ))

  implicit val referenceStatsWrites =
    Writes[ReferenceStats](stats =>
      Json.toJson(stats.aggregation.map { t => (t._1.toString, t._2._2.toSeq) }))

}
