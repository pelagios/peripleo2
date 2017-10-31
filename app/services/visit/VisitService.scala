package services.visit

import com.sksamuel.elastic4s.{ HitAs, RichSearchHit }
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.source.Indexable
import javax.inject.{ Inject, Singleton }
import play.api.Logger
import play.api.libs.json.Json
import scala.concurrent.{ Future, ExecutionContext }
import es.ES

@Singleton
class VisitService @Inject() (val es: ES, implicit val ctx: ExecutionContext) {
  
  implicit object VisitIndexable extends Indexable[Visit] {
    override def json(v: Visit): String = Json.stringify(Json.toJson(v))
  }

  implicit object VisitHitAs extends HitAs[Visit] {
    override def as(hit: RichSearchHit): Visit =
      Json.fromJson[Visit](Json.parse(hit.sourceAsString)).get
  }
  
  def insertVisit(visit: Visit): Future[Unit] =
    es.client execute {
      index into ES.PERIPLEO / ES.VISIT source visit
    } map { _ => 
    } recover { case t: Throwable =>
      Logger.error("Error logging visit event")
      val foo = t.printStackTrace
    }
    
  def countTotal(): Future[Long] =
    es.client execute {
      search in ES.PERIPLEO / ES.VISIT limit 0
    } map { _.totalHits }
    
  def countTotalSince(since: TimeInterval) = {
    
    import TimeInterval._
    
    val expression = since match {
      case LAST_24HRS => "now-24h"
      case LAST_7DAYS => "now-7d"
    }
    
    es.client execute {
      search in ES.PERIPLEO / ES.VISIT query {
        constantScoreQuery {
          filter ( rangeQuery("visited_at") from expression )
        }
      } size 0 aggregations (
        aggregation terms "top_items"    field "selection.identifier" size 10,
        aggregation terms "top_datasets" field "selection.is_in_dataset.ids" size 10,
        aggregation terms "top_searches" field "search.query.raw" size 10 
      )
    } map { response =>
      
      play.api.Logger.info(response.toString)
      
    }
    
  }
  
}

sealed trait TimeInterval

object TimeInterval {
  
  case object TODAY      extends TimeInterval
  case object LAST_24HRS extends TimeInterval
  case object LAST_7DAYS extends TimeInterval
  case object THIS_WEEK  extends TimeInterval
  case object THIS_MONTH extends TimeInterval
  
}
