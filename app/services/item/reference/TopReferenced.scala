package services.item.reference

import com.sksamuel.elastic4s.searches.aggs.RichAggregations
import java.util.UUID
import es.ES
import org.elasticsearch.search.aggregations.Aggregations
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import services.item.{Item, ItemService, ItemType}
import services.notification._
import services.HasNullableSeq

case class TopReferenced private (resolved: Seq[(ItemType, Seq[(Item, Long, Seq[(Relation.Value, Long)])])]) {
  
  def topN(itemType: ItemType) = resolved.find(_._1 == itemType).map(_._2.map(t => (t._1, t._2))).getOrElse(Seq.empty[(Item, Long)])
  
  def count(itemType: ItemType) = resolved.find(_._1 == itemType).map(_._2.size).getOrElse(0)
  
}

case class UnresolvedTopReferenced private (parsed: Seq[(ItemType, Seq[(UUID, Long, Seq[(Relation.Value, Long)])])]) {

  private def logError(triedIds: Seq[UUID], resolvedItems: Seq[Item])(implicit notifications: NotificationService) = {
    val failedIds = triedIds diff resolvedItems.map(_.docId)
    Logger.error("Error resolving related items:")
    Logger.error(failedIds.mkString(", "))
    Logger.error(failedIds.size + " out of " + triedIds.size)

    notifications.insertNotification(Notification(
      NotificationType.SYSTEM_ERROR, DateTime.now, None,
      "Failed to resolve " + failedIds.mkString(", ") + " (" + failedIds.size + " out of " + triedIds.size + ")"))
  }

  def resolve()(implicit es: ES, ctx: ExecutionContext, notifications: NotificationService): Future[TopReferenced] =  {
    val uuidsToResolve = parsed.flatMap(_._2.map(_._1))

    ItemService.resolveItems(uuidsToResolve).map { items =>
      val asTable = items.map { item => (item.docId, item) }.toMap

      val resolved = parsed.map { case (relation, buckets) =>
        val resolvedBuckets = buckets.flatMap { case (id, count, byRelation) =>
          asTable.get(id).map(item => (item, count, byRelation)) }

        (relation, resolvedBuckets)
      }

      // Cross-check if all items were resolved
      val resolvedItems = resolved.flatMap(_._2.map(_._1))
      if (resolvedItems.size < uuidsToResolve.size)
        logError(uuidsToResolve, resolvedItems)

      TopReferenced(resolved)
    }
  }

}

object TopReferenced extends HasNullableSeq {

  implicit val byRelationWrites = Writes[Seq[(Relation.Value, Long)]] { byRelation =>
    val asMap = byRelation.map(t => (t._1.toString, t._2)).toMap
    Json.toJson(asMap)
  }

  implicit val itemBucketWrites: Writes[(Item, Long, Seq[(Relation.Value, Long)])] = (
    (JsPath).write[Item] and
    (JsPath \ "referenced_count" \ "total").write[Long] and
    (JsPath \ "referenced_count" \ "by_relation").writeNullable[Seq[(Relation.Value, Long)]]
      .contramap[Seq[(Relation.Value, Long)]](toOptSeq)
  )(t => (t._1, t._2, t._3))

  implicit val topReferencedWrites =
    Writes[TopReferenced] { related =>
      val asMap = related.resolved.map(t => (t._1.toString, t._2)).toMap
      Json.toJson(asMap)
    }

  def parseAggregation(aggregations: RichAggregations): UnresolvedTopReferenced = {

    // Shorthand
    // TODO quick hack for ES2-to-5 migration clean up
    def getBucketsA(aggs: RichAggregations, key: String) = aggs.termsResult(key).getBuckets.asScala.toSeq
    def getBucketsB(aggs: Aggregations, key: String) = aggs.get[Terms](key).getBuckets.asScala.toSeq

    // First aggregation level by reference type (PLACE, PERSON, etc.)
    val parsed = getBucketsA(aggregations, "by_related").map { typeBucket =>
      val itemType = ItemType.withName(typeBucket.getKeyAsString)

      // Second aggregation level by item docID
      val byDocId = getBucketsB(typeBucket.getAggregations, "by_doc_id").map { docIdBucket =>
        val docId = UUID.fromString(docIdBucket.getKeyAsString)
        val countByDocId = docIdBucket.getDocCount

        // Third aggregation level by relation
        val byRelation = getBucketsB(docIdBucket.getAggregations, "by_relation").map { relationBucket =>
          val relation = Relation.withName(relationBucket.getKeyAsString)
          val countByRelation = relationBucket.getDocCount
          (relation, countByRelation)
        }

        (docId, countByDocId, byRelation)
      }

      (itemType, byDocId)
    }

    UnresolvedTopReferenced(parsed)
  }

}
