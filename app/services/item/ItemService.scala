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
  
  // TODO where do we remove references that point to entities not know to Peripleo? (here, I guess)
  
  def insertOrUpdateItem(item: Item, references: Seq[Reference]): Future[Boolean] =
    es.client execute {
      bulk (
         { update id item.identifiers.head in ES.PERIPLEO / ES.ITEM source item docAsUpsert } +: references.map{ ref =>
           update id item.identifiers.head in ES.PERIPLEO / ES.REFERENCE source ref parent item.identifiers.head docAsUpsert }
      )
    } map { _ => true
    } recover { case t: Throwable =>
      Logger.error("Error indexing item " + item.identifiers.head + ": " + t.getMessage)
      // t.printStackTrace
      false
    }
    
  override def importRecord(tuple: (Item, Seq[Reference])): Future[Boolean] =
    insertOrUpdateItem(tuple._1, tuple._2)
  
}