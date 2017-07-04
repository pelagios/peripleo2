package services.item.reference

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ HitAs, RichSearchHit, RichSearchResponse }
import com.sksamuel.elastic4s.source.Indexable
import java.util.UUID
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import play.api.Logger
import play.api.libs.json.Json
import scala.concurrent.Future
import scala.util.Try
import services.{ ES, Page }
import services.item.{ Item, ItemService }

trait ReferenceService { self: ItemService =>
  
  private implicit object RefHitAs extends HitAs[Reference] {
    override def as(hit: RichSearchHit): Reference = {
      Json.fromJson[Reference](Json.parse(hit.sourceAsString)).get
    }
  }

  private implicit object RefAndIdsHitAs extends HitAs[(Reference, String, String)] {
    override def as(hit: RichSearchHit): (Reference, String, String) = {
      val id = hit.id
      val parent = hit.field("_parent").value[String]
      val ref = Json.fromJson[Reference](Json.parse(hit.sourceAsString)).get
      (ref, id, parent)
    }
  }

  /** Turns unbound references into bound ones, filtering those that are unresolvable **/
  private[item] def resolveReferences(unbound: Seq[UnboundReference]): Future[Seq[Reference]] = {

    import ItemService._

    if (unbound.isEmpty)
      Future.successful(Seq.empty[Reference])
    else
      es.client execute {
        search in ES.PERIPLEO / ES.ITEM query {
          bool {
            // TODO this fetches all referenced items on a single item in one go
            // TODO we may need to limit this in some cases (literature!)
            // TODO current max-size is 10.000 unique (!) references
            should (
              unbound.map(_.uri).distinct.map(termQuery("is_conflation_of.identifiers", _))
            )
          }
        } start 0 limit ES.MAX_SIZE
      } map { response =>
        val items = response.as[(Item, Long)].map(_._1)
        unbound.flatMap { reference =>
          items.find(_.identifiers.contains(reference.uri)).map { case item =>
            reference.toReference(item.docId, item.bbox)
          }
        }
      }
  }

  def rewriteReferencesTo(itemsBeforeUpdate: Seq[Item], itemsAfterUpdate: Seq[Item]): Future[Boolean] = {

    import ItemService._

    def fetchNextBatch(scrollId: String): Future[RichSearchResponse] =
      es.client execute {
        search scroll scrollId keepAlive "5m"
      }

    def rewriteOne(ref: Reference, id: String, parent: String) =
      itemsBeforeUpdate.find(_.identifiers.contains(ref.referenceTo.uri)).map(_.docId) match {
        case Some(oldDestination) =>
          val newDestination = itemsAfterUpdate.find(_.identifiers.contains(ref.referenceTo.uri)).get
          if (oldDestination != newDestination.docId) {
            Logger.debug("Rewriting reference: " + ref.referenceTo.uri)
            val updatedReference = ref.rebind(newDestination.docId, newDestination.bbox)
            es.client execute {
              bulk (
                delete id id from ES.PERIPLEO / ES.REFERENCE parent parent,
                index into ES.PERIPLEO / ES.REFERENCE source updatedReference parent parent
              )
            } map { !_.hasFailures
            } recover { case t: Throwable => false }
          } else {
            Future.successful(true)
          }
          
        case None =>
          // Orphaned reference from previous failure? Clean up!
          Logger.warn("Cleaning up orphaned reference")
          
          es.client execute {
            delete id id from ES.PERIPLEO / ES.REFERENCE parent parent
          } map { _ => true } recover { case t: Throwable => false }
      }

    def rewriteBatch(response: RichSearchResponse, cursor: Long = 0l): Future[Boolean] = {
       val total = response.totalHits
       val refs = response.as[(Reference, String, String)].toSeq

       if (refs.isEmpty) {
         Future.successful(true)
       } else {
         val fSuccesses = Future.sequence(refs.map { case (ref, id, parent) => rewriteOne(ref, id, parent) })
         fSuccesses.flatMap { successes =>
           val success = !successes.exists(_ == false)
           val rewrittenTags = cursor + refs.size
           if (rewrittenTags < total)
             fetchNextBatch(response.scrollId).flatMap { response =>
               rewriteBatch(response, rewrittenTags).map { _ && success }
             }
           else
             Future.successful(success)
         }
       }
    }

    if (itemsBeforeUpdate.size > 0) {
      es.client execute {
        search in ES.PERIPLEO / ES.REFERENCE query {
          constantScoreQuery {
            filter (
              should (
                itemsBeforeUpdate.map(item => termQuery("reference_to.doc_id", item.docId.toString))
              )
            )
          }
        } limit 50 scroll "5m"
      } flatMap {
        rewriteBatch(_)
      }
    } else {
      Future.successful(true)
    }
  }
  
  /** Retrieves references by parent URI, destination URI, and context query **/ 
  def findReference(parentUri: String, destinationUri: String, query: String, offset: Int = 0, limit: Int = 20) =
    es.client execute {
      search in ES.PERIPLEO / ES.REFERENCE query {
        bool {
          must (
            termQuery("parent_uri" -> parentUri),
            termQuery("reference_to.uri" -> destinationUri),
            queryStringQuery(query).field("context")
          )
        }
      } highlighting (
        highlight field "context" fragmentSize 200
      )
    } map { response =>
      Page(response.tookInMillis, response.totalHits, offset, limit, response.as[Reference])
    }

  // TODO rename - there's currently some ambiguity (also in the API) between *references* and *stats about references*
  def getReferenceStats(identifier: String): Future[ReferenceStats] = {

    val fStats =
      es.client execute {
        search in ES.PERIPLEO / ES.REFERENCE query {
          hasParentQuery(ES.ITEM) query(termQuery("is_conflation_of.identifiers", identifier))
        } start 0 limit 0 aggregations (
          // Aggregate by reference type (PLACE | PERSON | PERIOD)
          aggregation terms "by_type" field "reference_type" aggregations (
            // Sub-aggregate by root URI
            aggregation terms "by_doc_id" field "reference_to.doc_id" size ES.MAX_SIZE aggregations (
              // Sub-sub-aggregate by relation
              aggregation terms "by_relation" field "relation" size 10
            )
          )
        )
      } map { response =>

        import scala.collection.JavaConverters._

        val byType = response.aggregations.get("by_type").asInstanceOf[Terms]
        byType.getBuckets.asScala.map { bucket =>
          val byDocId = bucket.getAggregations.get("by_doc_id").asInstanceOf[Terms]
          val byDocIdAsMap = byDocId.getBuckets.asScala.map { subBucket =>
            (subBucket.getKeyAsString, subBucket.getDocCount) // URI -> count
          }.toMap

          val referenceType = ReferenceType.withName(bucket.getKeyAsString)
          (referenceType, (bucket.getDocCount, byDocIdAsMap))
          
          // TODO parse by-relation info from response
        }.toMap
      }

    for {
      stats <- fStats
      // TODO horrible intermediate hack! Flattens place URIs from stats
      places <- ItemService.resolveItems(stats.flatMap(_._2._2.map(_._1)).toSeq)(self.es, self.ctx)
    } yield(ReferenceStats.build(stats, places.toSet))
  }

}
