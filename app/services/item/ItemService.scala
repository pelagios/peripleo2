package services.item

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ HitAs, RichSearchHit, RichSearchResponse, QueryDefinition }
import com.sksamuel.elastic4s.source.Indexable
import java.util.UUID
import javax.inject.{ Inject, Singleton }
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json.{ Json, JsSuccess, JsError }
import scala.concurrent.{ ExecutionContext, Future }
import services.{ ES, Page }
import services.item.reference.{ Reference, ReferenceService, UnboundReference }
import services.notification.NotificationService
import services.task.TaskType

object ItemService {

  import com.sksamuel.elastic4s.ElasticDsl.search // Otherwise there's ambiguity with the .search package!

  case class ItemWithReferences(item: Item, version: Long, references: Seq[Reference])

  case class ItemWithUnboundReferences(item: Item, maybeVersion: Option[Long], references: Seq[UnboundReference])

  implicit object ItemIndexable extends Indexable[Item] {
    override def json(i: Item): String = Json.stringify(Json.toJson(i))
  }

  implicit object ItemHitAs extends HitAs[(Item, Long)] {
    override def as(hit: RichSearchHit): (Item, Long) = {
      val item = Json.fromJson[Item](Json.parse(hit.sourceAsString)).get
      (item, hit.version)
    }
  }

  implicit object ReferenceIndexable extends Indexable[Reference] {
    override def json(r: Reference): String = Json.stringify(Json.toJson(r))
  }

  def resolveItems(ids: Seq[UUID])(implicit es: ES, ctx: ExecutionContext): Future[Seq[Item]] =
    if (ids.isEmpty)
      Future.successful(Seq.empty[Item])
    else
      es.client execute {
        multiget ( ids.map(id => get id id.toString from ES.PERIPLEO / ES.ITEM ) )
      } map {_.responses.flatMap { _.response.flatMap { response =>
        Option(response.getSourceAsString).flatMap(json =>
          Json.fromJson[Item](Json.parse(json)).asOpt)
      }}}

}

@Singleton
class ItemService @Inject() (
  val notifications: NotificationService,
  val es: ES,
  implicit val ctx: ExecutionContext
) extends ReferenceService {

  import ItemService._
  import com.sksamuel.elastic4s.ElasticDsl.search // Otherwise there's ambiguity with the .search package

  def findByIdentifier(identifier: String) =
    es.client execute {
      search in ES.PERIPLEO / ES.ITEM query {
        constantScoreQuery {
          filter ( termQuery("is_conflation_of.identifiers" -> identifier) )
        }
      }
    } map { _.as[(Item, Long)].headOption.map(_._1) }
    
  def findPartsOf(identifier: String, offset: Int = 0, limit: Int = 20) =
    es.client execute {
      search in ES.PERIPLEO / ES.ITEM query {
        constantScoreQuery {
          filter ( termQuery("is_conflation_of.is_part_of.ids" -> identifier) )
        }
      } start offset limit limit
    } map { response =>
      Page(response.tookInMillis, response.totalHits, offset, limit, response.as[(Item, Long)].map(_._1))
    }

  /** Retrieves connected items.
    *
    * Items are connected of they match any of the provided URIs in
    * their identifiers, close_match or exact_match fields.
    *
    * TODO make this method filter-able by item type
    *
    * TODO this method will only retrieve up to ES.MAX_SIZE references - however
    * we *might* have items with more than that in extreme cases (e.g. will Strabo reference
    * more than 10k unique places?)
    *
    */
  def findConnected(uris: Seq[String]): Future[Seq[ItemWithReferences]] = {
    val queryClause =
      constantScoreQuery {
        filter (
          should {
            uris.map(uri => termQuery("is_conflation_of.identifiers" -> uri)) ++
            uris.map(uri => termQuery("is_conflation_of.close_matches" -> uri)) ++
            uris.map(uri => termQuery("is_conflation_of.exact_matches" -> uri))
          }
        )
      }

    val fItems: Future[Seq[(Item, Long)]] =
      es.client execute {
        search in ES.PERIPLEO / ES.ITEM query queryClause version true limit ES.MAX_SIZE
      } map { _.as[(Item, Long)].toSeq }

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

    def group(items: Seq[(Item, Long)], references: Seq[(Reference, UUID)]): Seq[ItemWithReferences] = {
      val byParentId = references.groupBy(_._2).mapValues(_.map(_._1))
      items.map { case (item, version) =>
        ItemWithReferences(item, version, byParentId.get(item.docId).getOrElse(Seq.empty[Reference])) }
    }

    for {
      items <- fItems
      references <- fReferences
    } yield group(items, references)
  }

  def deleteById(docId: UUID): Future[Boolean] = {
    // Delete all references this item has (start operation right now, using 'val')
    val fDeleteReferences =
      es.deleteByQuery(ES.REFERENCE, termQuery("parent_id", docId.toString), Some(docId.toString))

    // Should start after delete is done (using 'def')
    def fDeleteItem() =
      es.client execute {
        delete id docId.toString from ES.PERIPLEO / ES.ITEM
      } map { _.isFound }

    val f = for {
      deleteRefsSuccess <- fDeleteReferences
      if (deleteRefsSuccess)
      deleteItemSuccess <- fDeleteItem()
    } yield deleteItemSuccess

    f.recover { case t: Throwable =>
      Logger.error("Error deleting item " + docId + ": " + t.getMessage)
      // t.printStackTrace
      false
    }
  }

  def deleteByDataset(dataset: String) = {

    def deleteReferences(): Future[Boolean] =
      es.deleteByQuery(ES.REFERENCE, hasParentQuery(ES.ITEM) query {
        termQuery("is_in_dataset", dataset)
      })

    def deleteObjects(): Future[Boolean] =
      es.deleteByQuery(ES.ITEM, termQuery("is_in_dataset", dataset))

    def deleteDatasets(): Future[Boolean] =
      es.deleteByQuery(ES.ITEM, bool {
        should(
          termQuery("identifiers", dataset),
          termQuery("is_part_of", dataset)
        )
      })

    for {
      s1 <- deleteReferences
      s2 <- deleteObjects
      s3 <- deleteDatasets
    } yield s1 && s2 && s3

  }

}
