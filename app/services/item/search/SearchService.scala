package services.item.search

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ HitAs, RichSearchHit }
import javax.inject.{ Inject, Singleton }
import play.api.libs.json.Json
import scala.concurrent.{ Future, ExecutionContext }
import services.{ ES, Page }
import services.item.Item

@Singleton
class SearchService @Inject() (val es: ES, implicit val ctx: ExecutionContext) {
  
  // TODO support open intervals
  private val TIME_HISTOGRAM_SCRIPT = 
    """
    from = doc['temporal_bounds.from']
    to = doc['temporal_bounds.to']
    if (from.empty || to.empty) [] else (from.date.year..to.date.year)
    """

  implicit object ItemHitAs extends HitAs[Item] {
    override def as(hit: RichSearchHit): Item =
      Json.fromJson[Item](Json.parse(hit.sourceAsString)).get
  }

  def query(args: SearchArgs): Future[Page[Item]] = {
    val searchDefinition =
      args.query match {
        case Some(q) => search in ES.PERIPLEO / ES.ITEM query q
        case None => search in ES.PERIPLEO / ES.ITEM
      }

    es.client execute {
      searchDefinition start args.offset limit args.limit aggregations (
        aggregation
          terms "by_type"
          field "item_type"
          size 20,
          
        aggregation
          histogram "by_century"
          interval 100
          script TIME_HISTOGRAM_SCRIPT
      )
    } map { response =>

      // TODO parse & wrap aggregation results
      play.api.Logger.info(response.toString)

      val items = response.as[Item].toSeq
      Page(response.tookInMillis, response.totalHits, args.offset, args.limit, items)
    }
  }

}
