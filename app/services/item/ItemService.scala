package services.item

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ HitAs, RichSearchHit, RichSearchResponse, QueryDefinition }
import com.sksamuel.elastic4s.source.Indexable
import javax.inject.{ Inject, Singleton }
import play.api.Logger
import play.api.libs.json.Json
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.postfixOps 
import services.{ ES, HasBatchImport }
import services.task.TaskType

@Singleton
class ItemService @Inject() (val es: ES, implicit val ctx: ExecutionContext) extends HasBatchImport[(Item, Seq[Reference])] {

  override val taskType = TaskType("ITEM_IMPORT")
  
  implicit object ItemIndexable extends Indexable[Item] {
    override def json(i: Item): String = Json.stringify(Json.toJson(i))
  }

  implicit object ItemHitAs extends HitAs[Item] {
    override def as(hit: RichSearchHit): Item =
      Json.fromJson[Item](Json.parse(hit.sourceAsString)).get
  }
  
  // TODO this method is duplicate in ItemService and PlaceService - refactor
  
  implicit object ReferenceIndexable extends Indexable[Reference] {
    override def json(r: Reference): String = Json.stringify(Json.toJson(r))
  }
  
  // TODO how to handle IDs for references, so we can update properly?
  
  /** Keeps only references that resolve to an entity in Peripleo **/
  def filterResolvable(references: Seq[Reference]): Future[Seq[Reference]] =
    if (references.isEmpty)
      Future.successful(Seq.empty[Reference])
    else
      es.client execute {
        multiget ( references.map(ref => get id ref.uri from ES.PERIPLEO / ES.ITEM) )
      } map { result => 
        val found = result.responses.map { r =>
          val found = r.response.map(_.isExists).getOrElse(false)
          (r.getId, found)
        }.toMap
        
        references.filter(ref => found.get(ref.uri).get)
      }

  def insertOrUpdateItem(item: Item, references: Seq[Reference]): Future[Boolean] = {
    val fFilterResolvable = filterResolvable(references)
    
    def fUpsert(resolvableReferences: Seq[Reference]) =
      es.client execute {
        bulk (
          { update id item.identifiers.head in ES.PERIPLEO / ES.ITEM source item docAsUpsert } +: 
            resolvableReferences.map{ ref =>
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
  
  private def deleteByQuery(q: QueryDefinition): Future[Unit] = {

    import com.sksamuel.elastic4s.ElasticDsl.search // Otherwise there's ambiguity with the .search package!
    
    def fetchNextBatch(scrollId: String): Future[RichSearchResponse] =
      es.client execute {
        search scroll scrollId keepAlive "1m"
      }
    
    def deleteOneBatch(ids: Seq[String]): Future[Unit] =
      es.client execute {
        bulk (
          ids.map { id => delete id id from ES.PERIPLEO / ES.ITEM }
        )
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
      search in ES.PERIPLEO / ES.ITEM query q limit 50 scroll "1m"
    } flatMap {
      deleteBatch(_)
    }
  }
    
  
  def deleteByDataset(dataset: String) = {
    
    def deleteObjects(): Future[Unit] =
      deleteByQuery(termQuery("is_in_dataset", dataset))
    
    def deleteDatasets(): Future[Unit] =
      deleteByQuery(bool { 
        should(
          termQuery("identifiers", dataset),
          termQuery("is_part_of", dataset)
        )
      })
    
    for {
      _ <- deleteObjects
      _ <- deleteDatasets
    } yield ()
    
  }
    
  override def importRecord(tuple: (Item, Seq[Reference])): Future[Boolean] =
    insertOrUpdateItem(tuple._1, tuple._2)
  
}