package services.item.place

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ HitAs, RichSearchHit, SearchType }
import com.sksamuel.elastic4s.source.Indexable
import javax.inject.{ Inject, Singleton }
import play.api.Logger
import play.api.libs.json.Json
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.{ postfixOps, reflectiveCalls }
import services.ES
import services.item.{ Reference, ReferenceType, Relation }

@Singleton
class PlaceService @Inject() (val es: ES, implicit val ctx: ExecutionContext) extends PlaceImporter {
  
  implicit object PlaceIndexable extends Indexable[Place] {
    override def json(p: Place): String = Json.stringify(Json.toJson(p))
  }

  implicit object PlaceHitAs extends HitAs[Place] {
    override def as(hit: RichSearchHit): Place =
      Json.fromJson[Place](Json.parse(hit.sourceAsString)).get
  }
  
  implicit object ReferenceIndexable extends Indexable[Reference] {
    override def json(r: Reference): String = Json.stringify(Json.toJson(r))
  }

  def insertOrUpdatePlace(place: Place): Future[Boolean] = {
    val ref = Reference(ReferenceType.PLACE, Some(Relation.COVERAGE), Some(place.rootUri), None)
        
    es.client execute {
      bulk (
        update id place.rootUri in ES.PERIPLEO / ES.ITEM source place docAsUpsert,
        update id place.rootUri in ES.PERIPLEO / ES.REFERENCE source ref parent place.rootUri docAsUpsert
      )
    } map { _ => true
    } recover { case t: Throwable =>
      Logger.error("Error indexing place " + place.rootUri + ": " + t.getMessage)
      // t.printStackTrace
      false
    }
  }
    
  def deletePlace(rootUri: String): Future[Boolean] =
    es.client execute {
      bulk (
        delete id rootUri from ES.PERIPLEO / ES.REFERENCE parent rootUri,
        delete id rootUri from ES.PERIPLEO / ES.ITEM
      )
    } map { response =>
      if (response.failures.size > 0) Logger.error(response.failureMessage)
      response.failures.size == 0
    } recover { case t: Throwable =>
      Logger.error("Error deleting place " + rootUri + ": " + t.getMessage)
      // t.printStackTrace
      false
    }
  
  def findByPlaceOrMatchURIs(uris: Seq[String]): Future[Seq[Place]] =
    es.client execute {
      search in ES.PERIPLEO / ES.ITEM query {
        nestedQuery("is_conflation_of").query {
          bool {
            should {
              uris.map(uri => termQuery("is_conflation_of.uri" -> uri)) ++
              uris.map(uri => termQuery("is_conflation_of.close_matches" -> uri)) ++
              uris.map(uri => termQuery("is_conflation_of.exact_matches" -> uri))
            }
          }
        }
      } limit 100 // TODO filter by type?
    } map { _.as[Place].toSeq }
    
  def findByRootUris(uris: Seq[String]) =
    es.client execute {
      multiget ( uris.map(uri => get id uri from ES.PERIPLEO / ES.ITEM) )
    } map { _.responses.flatMap { _.response.map(_.getSourceAsString).map { json =>
      Json.fromJson[Place](Json.parse(json)).get    
    }}}
    
}