package services.item.reference

import com.sksamuel.elastic4s.{Hit, HitReader}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.searches.RichSearchResponse
import es.ES
import java.util.UUID
import play.api.Logger
import play.api.libs.json.Json
import scala.concurrent.Future
import scala.util.Try
import services.Page
import services.item.{Item, ItemService, ItemType}

trait ReferenceService { self: ItemService =>

  private implicit object RefHitReader extends HitReader[Reference] {
    override def read(hit: Hit): Either[Throwable, Reference] =
      Right(Json.fromJson[Reference](Json.parse(hit.sourceAsString)).get)
  }
  
  protected def toRefAndIds(response: RichSearchResponse): Seq[(Reference, String, String)] =
    response.hits.map { hit =>
      val id = hit.id
      val parent = hit.java.getField("_parent").getValue[String]
      val ref = Json.fromJson[Reference](Json.parse(hit.sourceAsString)).get
      (ref, id, parent)
    }

  protected def toRefAndHighlight(response: RichSearchResponse): Seq[(Reference, Seq[String])] =
    response.hits.map { hit =>
      val reference = Json.fromJson[Reference](Json.parse(hit.sourceAsString)).get
      val snippets = hit.highlightFields.headOption
        .map(_._2.fragments.map(_.string.trim).toSeq.distinct)
        .getOrElse(Seq.empty[String])

      (reference, snippets)
    }

  /** Turns unbound references into bound ones, filtering those that are unresolvable **/
  private[item] def resolveReferences(unbound: Seq[UnboundReference]): Future[Seq[Reference]] = {

    import ItemService._
    
    val uris = unbound.map(_.uri).distinct

    if (uris.isEmpty)
      Future.successful(Seq.empty[Reference])
    else      
      es.client execute {
        search(ES.PERIPLEO / ES.ITEM) query {
          boolQuery
            // TODO this fetches all referenced items on a single item in one go
            // TODO we may need to limit this in some cases (literature!)
            // TODO current max-size is 10.000 unique (!) references
            should (
              uris.map(termQuery("is_conflation_of.identifiers", _)) ++
              uris.map(termQuery("is_conflation_of.links.uri", _))
            )
        } start 0 limit ES.MAX_SIZE
      } map { response =>
        val items = response.to[(Item, Long)].map(_._1)
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
        searchScroll(scrollId) keepAlive "5m"
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
                delete(id) from ES.PERIPLEO / ES.REFERENCE parent parent,
                indexInto(ES.PERIPLEO / ES.REFERENCE) source updatedReference parent parent
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
            delete(id) from ES.PERIPLEO / ES.REFERENCE parent parent
          } map { _ => true } recover { case t: Throwable => false }
      }

    def rewriteBatch(response: RichSearchResponse, cursor: Long = 0l): Future[Boolean] = {
       val total = response.totalHits
       val refs = toRefAndIds(response)

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
        search(ES.PERIPLEO / ES.REFERENCE) query {
          constantScoreQuery { 
            should (
              itemsBeforeUpdate.map(item => termQuery("reference_to.doc_id", item.docId.toString))
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

  /** Returns the references carried by the item with the given URI **/
  def getReferences(parentUri: String, destinationUri: Option[String], query: Option[String], offset: Int = 0, limit: Int = 20) = {

    def executeQuery(docId: Option[UUID]) = {
      val clauses =
        Seq(
          Some(termQuery("parent_uri" -> parentUri)),
          docId.map(id => termQuery("reference_to.doc_id" -> id.toString)),
          query.map(q => queryStringQuery(q).field("quote.context"))).flatten

      es.client execute {
        search(ES.PERIPLEO / ES.REFERENCE) query {
          boolQuery.must(clauses)
        } size offset limit limit highlighting (
          highlight("quote.context") fragmentSize 200
        )
      } map { response =>
        Page(response.tookInMillis, response.totalHits, offset, limit, toRefAndHighlight(response))
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
      search(ES.PERIPLEO / ES.REFERENCE) query {
        constantScoreQuery {
          termQuery("parent_uri" -> parentUri)
        }
      } aggs (
        cardinalityAggregation("distinct") field "reference_to.doc_id"
      ) size 0
    } map { response =>
      val c = response.aggregations.cardinalityResult("distinct")
      (response.totalHits, c.getValue.toInt)
    }
    
  /** Like above, but restricted to a specific destination item type  **/
  def countReferencesToType(parentUri: String, itemType: ItemType): Future[(Long, Int)] =
    es.client execute {
      search(ES.PERIPLEO / ES.REFERENCE) query {
        constantScoreQuery {
          boolQuery.must(
            termQuery("parent_uri" -> parentUri),
            termQuery("reference_to.item_type" -> itemType.toString)
          )
        }
      } aggs (
        cardinalityAggregation("distinct") field "reference_to.doc_id"
      ) size 0
    } map { response =>
      val c = response.aggregations.cardinalityResult("distinct")
      (response.totalHits, c.getValue.toInt)
    }

  def getTopReferenced(identifier: String): Future[TopReferenced] = {

    val fRelatedQuery =
      es.client execute {
        search(ES.PERIPLEO / ES.REFERENCE) query {
          constantScoreQuery {
            hasParentQuery(ES.ITEM) query(termQuery("is_conflation_of.identifiers", identifier)) scoreMode false
          }
        } start 0 limit 0 aggregations (
          // Aggregate by reference type (PLACE | PERSON | PERIOD)
          termsAggregation("by_related") field "reference_to.item_type" subaggs (
            // Sub-aggregate by docId
            termsAggregation("by_doc_id") field "reference_to.doc_id" size ES.MAX_SIZE subaggs (
              // Sub-sub-aggregate by relation
              termsAggregation("by_relation") field "relation" size 10
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
