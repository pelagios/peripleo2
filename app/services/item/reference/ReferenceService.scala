package services.item.reference

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ HitAs, RichSearchHit, RichSearchResponse }
import com.sksamuel.elastic4s.source.Indexable
import es.ES
import java.util.UUID
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import org.elasticsearch.search.aggregations.metrics.cardinality.InternalCardinality
import play.api.Logger
import play.api.libs.json.Json
import scala.concurrent.Future
import scala.util.Try
import services.{ Page }
import services.item.{ Item, ItemService }
import services.item.ItemType

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

  private implicit object RefAndHighlightHitAs extends HitAs[(Reference, Seq[String])] {
    override def as(hit: RichSearchHit): (Reference, Seq[String]) = {
      val reference = Json.fromJson[Reference](Json.parse(hit.sourceAsString)).get
      val snippets = hit.highlightFields.headOption
        .map(_._2.fragments.map(_.string.trim).toSeq.distinct)
        .getOrElse(Seq.empty[String])

      (reference, snippets)
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
            reference.toReference(item)
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
            val updatedReference = ref.rebind(newDestination)
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

  def getReferences(parentUri: String, destinationUri: Option[String], query: Option[String], offset: Int = 0, limit: Int = 20) = {

    def executeQuery(docId: Option[UUID]) = {
      val clauses =
        Seq(
          Some(termQuery("parent_uri" -> parentUri)),
          docId.map(id => termQuery("reference_to.doc_id" -> id.toString)),
          query.map(q => queryStringQuery(q).field("quote.context"))).flatten

      es.client execute {
        search in ES.PERIPLEO / ES.REFERENCE query {
          bool {
            must ( clauses )
          }
        } size offset limit limit highlighting (
          highlight field "quote.context" fragmentSize 200
        )
      } map { response =>
        Page(response.tookInMillis, response.totalHits, offset, limit, response.as[(Reference, Seq[String])])
      }

    }

    destinationUri match {

      // Query is filtered by destination URI
      case Some(uri) => for {

        // Resolve destination URI
        destinationItem <- self.findByIdentifier(uri)

        // If destination item exists, execute query, else return empty page
        references <- destinationItem match {

          case Some(item) =>
            executeQuery(Some(item.docId))

          case None =>
            Future.successful(Page.empty[(Reference, Seq[String])])

        }

      } yield (references)


      // Query is not filtered by destination URI - execute
      case None =>
        executeQuery(None)
    }

  }
  
  /** 
   *  For parent item, this method returns 
   *  
   *  i) the total number of references
   *  ii) the number of unique referenced items
   */
  def countReferences(parentUri: String): Future[(Long, Int)] =
    es.client execute {
      search in ES.PERIPLEO / ES.REFERENCE query {
        constantScoreQuery {
          filter ( termQuery("parent_uri" -> parentUri) )
        }
      } aggs (
        aggregation cardinality "distinct" field "reference_to.doc_id"
      ) size 0
    } map { response =>
      val c = response.aggregations.get("distinct").asInstanceOf[InternalCardinality]
      (response.totalHits, c.getValue.toInt)
    }
    
  /** Like above, but restricted to a specific destination item type  **/
  def countReferencesToType(parentUri: String, itemType: ItemType): Future[(Long, Int)] =
    es.client execute {
      search in ES.PERIPLEO / ES.REFERENCE query {
        constantScoreQuery {
          filter ( 
            bool {
              must (
                termQuery("parent_uri" -> parentUri),
                termQuery("reference_to.item_type" -> itemType.toString)
              )
            }
          )
        }
      } aggs (
        aggregation cardinality "distinct" field "reference_to.doc_id"
      ) size 0
    } map { response =>
      val c = response.aggregations.get("distinct").asInstanceOf[InternalCardinality]
      (response.totalHits, c.getValue.toInt)
    }

  def getTopReferenced(identifier: String): Future[TopReferenced] = {

    val fRelatedQuery =
      es.client execute {
        search in ES.PERIPLEO / ES.REFERENCE query {
          constantScoreQuery {
            filter ( hasParentQuery(ES.ITEM) query(termQuery("is_conflation_of.identifiers", identifier)) )
          }
        } start 0 limit 0 aggregations (
          // Aggregate by reference type (PLACE | PERSON | PERIOD)
          aggregation terms "by_related" field "reference_to.item_type" aggregations (
            // Sub-aggregate by docId
            aggregation terms "by_doc_id" field "reference_to.doc_id" size ES.MAX_SIZE aggregations (
              // Sub-sub-aggregate by relation
              aggregation terms "by_relation" field "relation" size 10
            )
          )
        )
      } map { response =>
        TopReferenced.parseAggregation(response.aggregations)
      }

    for {
      unresolvedRelated <- fRelatedQuery
      related <- unresolvedRelated.resolve()(self.es, self.ctx, self.notifications)
    } yield(related)
  }

}
