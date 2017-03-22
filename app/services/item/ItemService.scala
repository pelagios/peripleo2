package services.item

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ HitAs, RichSearchHit, RichSearchResponse, QueryDefinition }
import com.sksamuel.elastic4s.source.Indexable
import javax.inject.{ Inject, Singleton }
import play.api.Logger
import play.api.libs.json.Json
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.postfixOps 
import services.{ ES, HasBatchImport, Page }
import services.task.TaskType
import org.elasticsearch.search.aggregations.bucket.terms.Terms

@Singleton
class ItemService @Inject() (val es: ES, implicit val ctx: ExecutionContext) extends HasBatchImport[(Item, Seq[Reference])] {
  
  import com.sksamuel.elastic4s.ElasticDsl.search // Otherwise there's ambiguity with the .search package!

  override val taskType = TaskType("ITEM_IMPORT")
  
  implicit object ItemIndexable extends Indexable[Item] {
    override def json(i: Item): String = Json.stringify(Json.toJson(i))
  }

  implicit object ItemHitAs extends HitAs[(Item, String)] {
    override def as(hit: RichSearchHit): (Item, String) = {
      val item = Json.fromJson[Item](Json.parse(hit.sourceAsString)).get
      val id = hit.getId
      (item, id)
    }
  }
  
  // TODO this method is duplicate in ItemService and PlaceService - refactor
  
  implicit object ReferenceIndexable extends Indexable[Reference] {
    override def json(r: Reference): String = Json.stringify(Json.toJson(r))
  }
  
  /** Keeps only references that resolve to an entity in Peripleo **/
  private def filterResolvable(references: Seq[Reference]): Future[Seq[Reference]] =
    if (references.isEmpty)
      Future.successful(Seq.empty[Reference])
    else
      es.client execute {
        search in ES.PERIPLEO / ES.ITEM query {
          bool {
            // TODO this fetches all referenced items on a single item in one go
            // TODO we may need to limit this in some cases (literature!) 
            // TODO current max-size is 10.000 unique (!) references
            should (
              references.map(_.uri).distinct.map(termQuery("identifiers", _))
            )
          }
        } start 0 limit ES.MAX_SIZE
      } map { response =>
        // Re-write reference parentIds according to index content
        val items = response.as[(Item, String)]
        references.flatMap { reference =>
          items.find(_._1.identifiers.contains(reference.uri)).map { case (item, id) =>
            reference.copy(rootUri = id)
          }
        }
      }

  def insertOrUpdateItem(item: Item, references: Seq[Reference]): Future[Boolean] = {
    val fFilterResolvable = filterResolvable(references)
    
    def fUpsert(resolvableReferences: Seq[Reference]) =
      es.client execute {
        bulk (
          { update id item.identifiers.head in ES.PERIPLEO / ES.ITEM source item docAsUpsert } +: 
            resolvableReferences.map { ref =>
              
              // TODO how to handle IDs for references, so we can update properly?
              update id item.identifiers.head in ES.PERIPLEO / ES.REFERENCE source ref parent item.identifiers.head docAsUpsert }
          
        )
      } map { _ => true }
      
    val f = for {
      resolvableReferences <- fFilterResolvable
      success <- fUpsert(resolvableReferences)
    } yield success
    
    f.recover { case t: Throwable =>
      Logger.error("Error indexing item " + item.identifiers.head + ": " + t.getMessage)
      // t.printStackTrace
      false
    }
  }
  
  def getReferenceStats(identifier: String): Future[ReferenceStats] =
    es.client execute {
      search in ES.PERIPLEO / ES.REFERENCE query {
        hasParentQuery(ES.ITEM) query(termQuery("identifiers", identifier))
      } start 0 limit 0 aggregations (
        // Aggregate by reference type...
        aggregation terms "by_type" field "reference_type" aggregations (
          // ... and sub-aggregate by root URI
          aggregation terms "by_root_uri" field "root_uri" size ES.MAX_SIZE
        )
      )
    } map { response =>
      
      import scala.collection.JavaConverters._
      
      val byType = response.aggregations.get("by_type").asInstanceOf[Terms]
      val byTypeAsMap = byType.getBuckets.asScala.map { bucket =>
        val byUri = bucket.getAggregations.get("by_root_uri").asInstanceOf[Terms]
        val byUriAsMap = byUri.getBuckets.asScala.map { subBucket =>
          // URI -> count
          (subBucket.getKeyAsString, subBucket.getDocCount)
        }.toMap
        
        val referenceType = ReferenceType.withName(bucket.getKeyAsString)
        (referenceType, (bucket.getDocCount, byUriAsMap))
      }.toMap

      ReferenceStats(identifier, byTypeAsMap)
    }
  
  private def deleteByQuery(index: String, q: QueryDefinition): Future[Unit] = {
    
    def fetchNextBatch(scrollId: String): Future[RichSearchResponse] =
      es.client execute {
        search scroll scrollId keepAlive "1m"
      }
    
    def deleteOneBatch(ids: Seq[String]): Future[Unit] =
      es.client execute {
        bulk ( ids.map { id => delete id id from ES.PERIPLEO / index } )
      } map { _ => () }
    
    def deleteBatch(response: RichSearchResponse, cursor: Long = 0l): Future[Unit] = {
      val ids = response.hits.map(_.getId)
      val total = response.totalHits
      
      deleteOneBatch(ids).flatMap { _ =>
        val deletedRecords = cursor + ids.size
        if (deletedRecords < total) {
          fetchNextBatch(response.scrollId).flatMap(deleteBatch(_, deletedRecords))
        } else {
          Future.successful((): Unit)
        }
      }
    }
    
    es.client execute {
      search in ES.PERIPLEO / index query q limit 50 scroll "1m"
    } flatMap {
      deleteBatch(_)
    }
  }
    
  
  def deleteByDataset(dataset: String) = {
    
    def deleteReferences(): Future[Unit] =
      deleteByQuery(ES.REFERENCE, hasParentQuery(ES.ITEM) query {
        termQuery("is_in_dataset", dataset) 
      })
    
    def deleteObjects(): Future[Unit] =
      deleteByQuery(ES.ITEM, termQuery("is_in_dataset", dataset))
    
    def deleteDatasets(): Future[Unit] =
      deleteByQuery(ES.ITEM, bool { 
        should(
          termQuery("identifiers", dataset),
          termQuery("is_part_of", dataset)
        )
      })
    
    for {
      _ <- deleteReferences
      _ <- deleteObjects
      _ <- deleteDatasets
    } yield ()
    
  }
    
  override def importRecord(tuple: (Item, Seq[Reference])): Future[Boolean] =
    insertOrUpdateItem(tuple._1, tuple._2)
  
}
