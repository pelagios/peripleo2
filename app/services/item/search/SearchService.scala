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
          histogram "by_year"
          interval 10
          
          // TODO this now generates matches at 1970 (0 in UNIX time) for items without temporal_bounds
          
          script "from = doc['temporal_bounds.from'].date.year; to = doc['temporal_bounds.to'].date.year; (from..to)"
      )
    } map { response =>

      // TODO parse & wrap aggregation results
      play.api.Logger.info(response.toString)

      val items = response.as[Item].toSeq
      Page(response.tookInMillis, response.totalHits, args.offset, args.limit, items)
    }
  }

}
