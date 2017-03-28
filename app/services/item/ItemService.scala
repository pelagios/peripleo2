package services.item

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ HitAs, RichSearchHit, RichSearchResponse, QueryDefinition }
import com.sksamuel.elastic4s.source.Indexable
import javax.inject.{ Inject, Singleton }
import play.api.libs.json.Json
import scala.concurrent.{ ExecutionContext, Future }
import services.ES
import services.item.reference.{ ReferenceService, UnboundReference }
import services.task.TaskType

@Singleton
class ItemService @Inject() (
  val es: ES,
  implicit val ctx: ExecutionContext
) extends ReferenceService with ItemImporter {
  
  import com.sksamuel.elastic4s.ElasticDsl.search // Otherwise there's ambiguity with the .search package!

  override val taskType = TaskType("ITEM_IMPORT")
  
  implicit object ItemIndexable extends Indexable[Item] {
    override def json(i: Item): String = Json.stringify(Json.toJson(i))
  }

  implicit object ItemHitAs extends HitAs[Item] {
    override def as(hit: RichSearchHit): Item = {
      Json.fromJson[Item](Json.parse(hit.sourceAsString)).get
    }
  }
  
  /** Retrieves connected items.
    * 
    * Items are connected of they match any of the provided URIs in
    * their identifiers, close_match or exact_match fields. 
    * 
    * TODO make this method filter-able by item type
    * 
    */
  def findConnected(uris: Seq[String]): Future[Seq[Item]] =
    es.client execute {
      search in ES.PERIPLEO / ES.ITEM query {
        bool {
          should {
            uris.map(uri => termQuery("is_conflation_of.identifiers" -> uri)) ++
            uris.map(uri => termQuery("is_conflation_of.close_matches" -> uri)) ++
            uris.map(uri => termQuery("is_conflation_of.exact_matches" -> uri))
          }
        }
      } limit ES.MAX_SIZE
    } map { _.as[Item].toSeq }
  
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
    
  override def importRecord(tuple: (Item, Seq[UnboundReference])): Future[Boolean] =
    insertOrUpdateItem(tuple._1, tuple._2)
  
}
