package services.item.place

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ HitAs, RichSearchHit, SearchType }
import com.sksamuel.elastic4s.source.Indexable
import javax.inject.{ Inject, Singleton }
import play.api.Logger
import play.api.libs.json.Json
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.postfixOps
import services.ES

@Singleton
class PlaceService @Inject() (val es: ES, implicit val ctx: ExecutionContext) extends PlaceImporter {
  
  implicit object PlaceIndexable extends Indexable[Place] {
    override def json(p: Place): String = Json.stringify(Json.toJson(p))
  }

  implicit object PlaceHitAs extends HitAs[Place] {
    override def as(hit: RichSearchHit): Place =
      Json.fromJson[Place](Json.parse(hit.sourceAsString)).get
  }

  def insertOrUpdatePlace(place: Place): Future[Boolean] =
    es.client execute {
      update id place.rootUri in ES.PERIPLEO/ ES.ITEM source place docAsUpsert
    } map { _ => true
    } recover { case t: Throwable =>
      Logger.error("Error indexing place " + place.rootUri + ": " + t.getMessage)
      // t.printStackTrace
      false
    }
  
  def findByPlaceOrMatchURIs(uris: Seq[String]): Future[Seq[(Place, Long)]] = ???
  
}