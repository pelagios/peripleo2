package services.item

import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{HitAs, RichSearchHit, RichSearchResponse, QueryDefinition}
import com.sksamuel.elastic4s.source.Indexable
import es.ES
import java.util.UUID
import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import org.elasticsearch.script.ScriptService.ScriptType
import play.api.Logger
import play.api.libs.json.{Json, JsSuccess, JsError}
import scala.concurrent.{ExecutionContext, Future}
import services.{Sort, Page}
import services.item.reference.{Reference, ReferenceService, UnboundReference}
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
    
  def findByHomepageURL(url: String) = 
    es.client execute {
      search in ES.PERIPLEO / ES.ITEM query {
        constantScoreQuery {
          filter ( termQuery("is_conflation_of.homepage" -> url) )
        }
      }
    } map { _.as[(Item, Long)].headOption.map(_._1) }
    
  def findByType(
    itemType : ItemType,
    rootOnly : Boolean = true,
    offset   : Int = 0,
    limit    : Int = 20,
    sort     : Option[Sort.Value] = None
  ) = {
    val f =
      if (rootOnly)
        bool {
          must (
            termQuery("item_type" -> itemType.toString)
          ) not (
            existsQuery("is_conflation_of.is_part_of")
          )
        }
      else
        termQuery("item_type" -> itemType.toString)
        
    val query = 
      sort match {
        case Some(sorting) =>
          search in ES.PERIPLEO / ES.ITEM query {
            constantScoreQuery { filter ( f ) }
          } start offset limit limit sort ( sorting )
          
        case None =>
          search in ES.PERIPLEO / ES.ITEM query {
            constantScoreQuery { filter ( f ) }
          } start offset limit limit
        }

    es.client execute {
      query
    } map { response =>
      Page(response.tookInMillis, response.totalHits, offset, limit, response.as[(Item, Long)].map(_._1))
    }
  }

  def findByIsPartOf(parent: ItemRecord, directChildrenOnly: Boolean = true, offset: Int = 0, limit: Int = 20) = {
    val parentId = parent.uri
    val ancestry = parent.isPartOf.map(_.ids).getOrElse(Seq.empty[String]) :+ parentId
    val query = 
      if (directChildrenOnly)
        // Will return children directly below the parent only, but not nested sub-levels
        bool { must (
          // Cf. https://www.elastic.co/guide/en/elasticsearch/guide/current/_finding_multiple_exact_values.html#_contains_but_does_not_equal
          termsQuery("is_conflation_of.is_part_of.ids", ancestry:_*),
          ScriptQueryDefinition(script("parent_count") params(Map("parents" -> ancestry.size))  scriptType ScriptType.FILE) 
        )}
      else 
        // Will return children at any level and sublevel
        termQuery("is_conflation_of.is_part_of.ids" -> parentId)
        
    es.client execute {
      search in ES.PERIPLEO / ES.ITEM query { 
        constantScoreQuery {
          filter ( query )
        }
      } start offset limit limit
    } map { response =>
      Page(response.tookInMillis, response.totalHits, offset, limit, response.as[(Item, Long)].map(_._1))
    }
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

  def updateItem(item: Item): Future[Boolean] = {
    es.client execute {
      update id item.docId.toString in ES.PERIPLEO / ES.ITEM source item
    } map { _ =>
      true
    } recover {
      case _ => false
    }
  }

  def deleteById(docId: UUID): Future[Boolean] = {
    // Delete all references this item has (start operation right now, using 'val')
    val fDeleteReferences = es.deleteChildrenByParent(ES.REFERENCE, docId.toString)

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

  /** Warning: this can break the conceptual integrity of the index, because
    * it might remove items that are referenced by other items that remain in the
    * index. Ideally, we should add a "safe delete" option that will check
    * whether references exist and then leaves items referenced by others (at
    * the expense of a huge performance slow-down, though!)
    */
  def fastDeleteByDataset(identifier: String) = {

    def deleteReferences(): Future[Boolean] = 
      es.deleteChildrenByQuery(ES.REFERENCE, hasParentQuery(ES.ITEM) query {
        termQuery("is_conflation_of.is_in_dataset" -> identifier)
      })

    def deleteObjects(): Future[Boolean] =
      es.deleteByQuery(ES.ITEM, termQuery("is_conflation_of.is_in_dataset" -> identifier))

    def deleteDatasets(): Future[Boolean] =
      es.deleteByQuery(ES.ITEM, bool {
        should(
          termQuery("identifiers" -> identifier),
          termQuery("is_conflation_of.is_part_of" -> identifier)
        )
      })

    for {
      s1 <- deleteReferences
      s2 <- deleteObjects
      s3 <- deleteDatasets
    } yield s1 && s2 && s3

  }

  def safeDeleteByDataset(identifier: String) = {
    
    play.api.Logger.info("Deleting dataset: " + identifier)

    // Refs pointing outwards from items in this dataset - can safely delete
    def deleteReferences(): Future[Boolean] =
      es.deleteChildrenByQuery(ES.REFERENCE, hasParentQuery(ES.ITEM) query {
        termQuery("reference_to.is_in_dataset", identifier)
      }) map { success =>
        play.api.Logger.info("References deleted: " + success)
        success
      } recover { case t: Throwable =>
        t.printStackTrace()
        false
      }

    // The condition we'll use to check if delete is safe: is
    // this item referenced by others (condition -> false) or not (condition -> true)
    def isOkToDelete(hit: RichSearchHit): Future[Boolean] =
      es.client execute {
        search in ES.PERIPLEO / ES.REFERENCE query {
          constantScoreQuery {
            filter ( termQuery("reference_to.doc_id" -> hit.id) )
          }
        } start 0 limit 0
      } map { _.totalHits == 0 }
      
    def deleteObjects(): Future[Boolean] =
      es.deleteConditional(ES.ITEM, termQuery("is_conflation_of.is_in_dataset.ids" -> identifier), isOkToDelete)
        .map { success =>
          play.api.Logger.info("Items deleted: " + success)
          success
        } recover { case t: Throwable =>
          t.printStackTrace()
          false
        }
        
    def hasItemsLeft(): Future[Boolean] =
      // Refresh index first, otherwise we'll still see items that were deleted in the meantime
      es.refreshIndex().flatMap { success =>
        if (success) {
          es.client execute {
            search in ES.PERIPLEO / ES.ITEM query {
              constantScoreQuery {
                filter ( termQuery("is_conflation_of.is_in_dataset.ids" -> identifier) )
              }
            }
          } map { _.totalHits > 0 }
        } else {
          Future.successful(false)
        }
      }

    // Datasets and subsets
    def deleteDatasets(): Future[Boolean] =
      es.deleteByQuery(ES.ITEM, bool {
        should(
          termQuery("is_conflation_of.identifiers" -> identifier),
          termQuery("is_conflation_of.is_part_of" -> identifier)
        )
      }) map { success =>
        play.api.Logger.info("Dataset items deleted: " + success)
        success
      } recover { case t: Throwable =>
        t.printStackTrace()
        false
      }

    for {
      s1 <- deleteReferences
      s2 <- deleteObjects
      
      // Check if the dataset was removed completely, or whether some 
      // items remained because they were referenced by others
      hasItemsLeft <- hasItemsLeft
      
      s3 <- if (hasItemsLeft) Future.successful(true) else deleteDatasets
    } yield s1 && s2

  }

}
