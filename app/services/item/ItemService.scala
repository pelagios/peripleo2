package services.item

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ HitAs, RichSearchHit, RichSearchResponse, QueryDefinition }
import com.sksamuel.elastic4s.source.Indexable
import java.util.UUID
import javax.inject.{ Inject, Singleton }
import play.api.Logger
import play.api.libs.json.Json
import scala.concurrent.{ ExecutionContext, Future }
import services.ES
import services.item.reference.{ Reference, ReferenceService, UnboundReference }
import services.task.TaskType

object ItemService {
  
  case class ItemWithReferences(item: Item, references: Seq[Reference])
  
  case class ItemWithUnboundReferences(item: Item, references: Seq[UnboundReference])
  
  implicit object ItemIndexable extends Indexable[Item] {
    override def json(i: Item): String = Json.stringify(Json.toJson(i))
  }

  implicit object ItemHitAs extends HitAs[Item] {
    override def as(hit: RichSearchHit): Item = {
      Json.fromJson[Item](Json.parse(hit.sourceAsString)).get
    }
  }
  
  implicit object ReferenceIndexable extends Indexable[Reference] {
    override def json(r: Reference): String = Json.stringify(Json.toJson(r))
  }
  
}

@Singleton
class ItemService @Inject() (
  val es: ES,
  implicit val ctx: ExecutionContext
) extends ReferenceService {
  
  import ItemService._
  import com.sksamuel.elastic4s.ElasticDsl.search // Otherwise there's ambiguity with the .search package!
  
  /** Retrieves connected items.
    * 
    * Items are connected of they match any of the provided URIs in
    * their identifiers, close_match or exact_match fields. 
    * 
    * TODO make this method filter-able by item type
    * 
    */
  def findConnected(uris: Seq[String]): Future[Seq[ItemWithReferences]] = {
    val queryClause =
      bool {
        should {
          uris.map(uri => termQuery("is_conflation_of.identifiers" -> uri)) ++
          uris.map(uri => termQuery("is_conflation_of.close_matches" -> uri)) ++
          uris.map(uri => termQuery("is_conflation_of.exact_matches" -> uri))
        }
      }
    
    val fItems: Future[Seq[Item]] = 
      es.client execute {
        search in ES.PERIPLEO / ES.ITEM query queryClause limit ES.MAX_SIZE
      } map { _.as[Item].toSeq }
    
    // It seems Elastic4s doesn't support inner hits on hasParentQueries at v2.4 :-(
    val fReferences: Future[Seq[(Reference, UUID)]] =
      es.client execute {
        search in ES.PERIPLEO / ES.REFERENCE query {
          hasParentQuery(ES.ITEM) query queryClause 
        } limit ES.MAX_SIZE
      } map { _.hits.toSeq.map { hit =>
        val reference = Json.fromJson[Reference](Json.parse(hit.sourceAsString)).get
        val parentId = UUID.fromString(hit.field("_parent").value[String])
        (reference, parentId)
      }}
    
    def group(items: Seq[Item], references: Seq[(Reference, UUID)]): Seq[ItemWithReferences] = {
      val byParentId = references.groupBy(_._2).mapValues(_.map(_._1))
      items.map(item =>
        ItemWithReferences(item, byParentId.get(item.docId).getOrElse(Seq.empty[Reference])))
    }
      
    for {
      items <- fItems
      references <- fReferences
    } yield group(items, references)
  }
  
  def deleteById(docId: UUID): Future[Boolean] = {
    val fDeleteReferences = 
      es.deleteByQuery(ES.REFERENCE, termQuery("reference_to.doc_id", docId.toString), Some(docId.toString))
      
    // Should start after delete is done
    def fDeleteItem() = 
      es.client execute {
        delete id docId.toString from ES.PERIPLEO / ES.ITEM
      }
    
    val f= for {
      _ <- fDeleteReferences
      _ <- fDeleteItem()
    } yield true
    
    f.recover { case t: Throwable =>
      Logger.error("Error deleting item " + docId + ": " + t.getMessage)
      // t.printStackTrace
      false
    }
  }

  def deleteByDataset(dataset: String) = {
    
    def deleteReferences(): Future[Unit] =
      es.deleteByQuery(ES.REFERENCE, hasParentQuery(ES.ITEM) query {
        termQuery("is_in_dataset", dataset) 
      })
    
    def deleteObjects(): Future[Unit] =
      es.deleteByQuery(ES.ITEM, termQuery("is_in_dataset", dataset))
    
    def deleteDatasets(): Future[Unit] =
      es.deleteByQuery(ES.ITEM, bool { 
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
      
}
