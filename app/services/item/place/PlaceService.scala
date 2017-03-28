package services.item.place

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ HitAs, RichSearchHit, SearchType }
import com.sksamuel.elastic4s.source.Indexable
import java.util.UUID
import javax.inject.{ Inject, Singleton }
import play.api.Logger
import play.api.libs.json.Json
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.{ postfixOps, reflectiveCalls }
import services.ES
import services.item.Item
import services.item.reference.{ Reference, ReferenceType, ReferenceTo } 

@Singleton
class PlaceService @Inject() (val es: ES, implicit val ctx: ExecutionContext) extends PlaceImporter {
  
  /**
   * 
   * TODO this is now complete redundancy with ItemService - resolve!
   * 
   */
  
  implicit object PlaceIndexable extends Indexable[Item] {
    override def json(p: Item): String = Json.stringify(Json.toJson(p))
  }

  implicit object PlaceHitAs extends HitAs[Item] {
    override def as(hit: RichSearchHit): Item =
      Json.fromJson[Item](Json.parse(hit.sourceAsString)).get
  }
  
  implicit object ReferenceIndexable extends Indexable[Reference] {
    override def json(r: Reference): String = Json.stringify(Json.toJson(r))
  }

  def insertOrUpdatePlace(place: Item): Future[Boolean] = {
    val ref = Reference(
      ReferenceType.PLACE,
      ReferenceTo(place.identifiers.head, place.docId),
      None, None, None, None)
        
    es.client execute {
      bulk (
        update id place.docId.toString in ES.PERIPLEO / ES.ITEM source place docAsUpsert,
        
        // TODO what should we use as ID for the place ref?
        update id place.docId.toString in ES.PERIPLEO / ES.REFERENCE source ref parent place.docId.toString docAsUpsert
      )
    } map { _ => true
    } recover { case t: Throwable =>
      Logger.error("Error indexing place " + place.identifiers.head + ": " + t.getMessage)
      // t.printStackTrace
      false
    }
  }
    
  def deletePlace(docId: UUID): Future[Boolean] =
    es.client execute {
      bulk (
        delete id docId.toString from ES.PERIPLEO / ES.REFERENCE parent docId.toString,
        delete id docId.toString from ES.PERIPLEO / ES.ITEM
      )
    } map { response =>
      if (response.failures.size > 0) Logger.error(response.failureMessage)
      response.failures.size == 0
    } recover { case t: Throwable =>
      Logger.error("Error deleting place " + docId.toString + ": " + t.getMessage)
      // t.printStackTrace
      false
    }
  
  def findByPlaceOrMatchURIs(uris: Seq[String]): Future[Seq[Item]] =
    es.client execute {
      search in ES.PERIPLEO / ES.ITEM query {
        bool {
          should {
            uris.map(uri => termQuery("is_conflation_of.identifiers" -> uri)) ++
            uris.map(uri => termQuery("is_conflation_of.close_matches" -> uri)) ++
            uris.map(uri => termQuery("is_conflation_of.exact_matches" -> uri))
          }
        }
      } limit 100 // TODO filter by type?
    } map { _.as[Item].toSeq }
    
}