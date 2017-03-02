package services.item

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ HitAs, RichSearchHit }
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
    
  override def importRecord(tuple: (Item, Seq[Reference])): Future[Boolean] =
    insertOrUpdateItem(tuple._1, tuple._2)
  
}