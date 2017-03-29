package services.item.reference

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.source.Indexable
import java.util.UUID
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import play.api.libs.json.Json
import scala.concurrent.Future
import services.ES
import services.item.{ Item, ItemService }

trait ReferenceService { self: ItemService =>
    
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
        val items = response.as[Item]
        unbound.flatMap { reference =>
          items.find(_.identifiers.contains(reference.uri)).map { case item =>
            reference.toReference(item.docId)
          }
        }
      }
  }
  
  def getReferenceStats(identifier: String): Future[ReferenceStats] = {
    
    /** TODO code duplication with SearchService!
      * 
      * We'll need to clean this up later, once we start introducing
      * people and periods as resolvable entities. When we get there, 
      * perhaps it's worth having a common "Entity" class, that has
      * only a small subset of the full item and/or a 'reference' sub-package
      * to keep things more orderly.  
      * 
      * TODO won't currently work - we need to retrieve by identifier not ElasticSearch ID
      * 
      */
    def resolvePlaces(uris: Seq[String]) = {
      if (uris.isEmpty)
        Future.successful(Seq.empty[Item])
      else
        es.client execute {
          multiget ( uris.map(uri => get id uri from ES.PERIPLEO / ES.ITEM) )
        } map { _.responses.flatMap { _.response.map(_.getSourceAsString).map { json =>
          Json.fromJson[Item](Json.parse(json)).get
        }}}
    }
    
    val fStats =
      es.client execute {
        search in ES.PERIPLEO / ES.REFERENCE query {
          hasParentQuery(ES.ITEM) query(termQuery("is_conflation_of.identifiers", identifier))
        } start 0 limit 0 aggregations (
          // Aggregate by reference type...
          aggregation terms "by_type" field "reference_type" aggregations (
            // ... and sub-aggregate by root URI
            aggregation terms "by_doc_id" field "reference_to.doc_id" size ES.MAX_SIZE
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
        }.toMap
      }
      
    for {
      stats <- fStats
      // TODO horrible intermediate hack! Flattens place URIs from stats
      places <- resolvePlaces(stats.flatMap(_._2._2.map(_._1)).toSeq)
    } yield(ReferenceStats.build(stats, places.toSet))
  }
  
}