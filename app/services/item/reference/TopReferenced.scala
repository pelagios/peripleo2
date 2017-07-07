package services.item.reference

import java.util.UUID
import org.elasticsearch.search.aggregations.Aggregations
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.collection.JavaConverters._
import scala.concurrent.{ ExecutionContext, Future }
import services.ES
import services.item.{ Item, ItemService }
import services.notification._

case class UnresolvedTopReferenced private (parsed: Seq[(ReferenceType.Value, Seq[(UUID, Long)])]) {
  
  private def logError(triedIds: Seq[UUID], resolvedItems: Seq[Item])(implicit notifications: NotificationService) = {
    val failedIds = triedIds diff resolvedItems.map(_.docId)
    Logger.error("Error resolving related items:")
    Logger.error(failedIds.mkString(", "))
    Logger.error(failedIds.size + " out of " + triedIds.size)
    
    notifications.insertNotification(Notification(
      NotificationType.SYSTEM_ERROR, DateTime.now,
      "Failed to resolve " + failedIds.mkString(", ") + " (" + failedIds.size + " out of " + triedIds.size + ")"))
  }
  
  def resolve()(implicit es: ES, ctx: ExecutionContext, notifications: NotificationService): Future[TopReferenced] =  {
    val uuidsToResolve = parsed.flatMap(_._2.map(_._1))
    
    ItemService.resolveItems(uuidsToResolve).map { items =>
      val asTable = items.map { item => (item.docId, item) }.toMap 
      
      val resolved = parsed.map { case (relation, keyVals) =>
        val resolvedKeyVals = keyVals.flatMap { case (id, count) =>
          asTable.get(id).map(item => (item, count)) }
        
        (relation, resolvedKeyVals)
      }
      
      // Cross-check if all items were resolved
      val resolvedItems = resolved.flatMap(_._2.map(_._1))
      if (resolvedItems.size < uuidsToResolve.size)
        logError(uuidsToResolve, resolvedItems)
      
      TopReferenced(resolved)
    }
  }
  
}

case class TopReferenced private (resolved: Seq[(ReferenceType.Value, Seq[(Item, Long)])])

object TopReferenced {
  
  implicit val itemCountWrites: Writes[(Item, Long)] = (
    (JsPath).write[Item] and
    (JsPath \ "related_count").write[Long]
  )(t => (t._1, t._2))
      
  implicit val topReferencedWrites = 
    Writes[TopReferenced] { related =>
      val asMap = related.resolved.map(t => (t._1.toString, t._2)).toMap
      Json.toJson(asMap) 
    }
  
  def parseAggregation(aggregations: Aggregations): UnresolvedTopReferenced = {
    
    // Shorthand
    def getBuckets(aggs: Aggregations, key: String) = aggs.get[Terms](key).getBuckets.asScala.toSeq 
      
    val parsed = getBuckets(aggregations, "by_related").map { bucket =>
      // First aggregation level by relation type (PLACE, PERSON, etc.)
      val relation = ReferenceType.withName(bucket.getKey.toString) 
      val subBuckets = getBuckets(bucket.getAggregations, "by_doc_id").map { subBucket =>
        val docId = UUID.fromString(subBucket.getKeyAsString)
        val count = subBucket.getDocCount
        
        (docId, count)
      }
      
      (relation, subBuckets)
    }
    
    UnresolvedTopReferenced(parsed)
  }
  
}