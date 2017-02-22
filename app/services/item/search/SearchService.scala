package services.item.search

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ HitAs, RichSearchHit }
import javax.inject.{ Inject, Singleton }
import play.api.libs.json.Json
import scala.concurrent.{ Future, ExecutionContext }
import services.{ ES, Page }
import services.item.Item
import org.elasticsearch.search.aggregations.bucket.histogram.InternalHistogram

@Singleton
class SearchService @Inject() (val es: ES, implicit val ctx: ExecutionContext) {
  
  private def histogramScript(interval: Int) =
    s"""
     f = doc['temporal_bounds.from']
     t = doc['temporal_bounds.to']
     buckets = []
     if (!(f.empty || t.empty))
       for (i=f.date.year; i<t.date.year; i+= $interval) { buckets.add(i) }
     buckets;
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
          terms "by_language"
          field "languages"
          size 20,
          
        aggregation
          histogram "by_decade"
          script histogramScript(10)
          interval 10,
          
        aggregation
          histogram "by_century"
          script histogramScript(100)
          interval 100 
      )
    } map { response =>
      val items = response.as[Item].toSeq

      val byCentury = Aggregation.parseHistogram(response.aggregations.get("by_century"), "by_time")
      val byDecade = Aggregation.parseHistogram(response.aggregations.get("by_decade"), "by_time")
      
      val aggregations = Seq(
        Aggregation.parseTerms(response.aggregations.get("by_type")),
        Aggregation.parseTerms(response.aggregations.get("by_language")),
        if (byCentury.buckets.size >= 20) byCentury else byDecade
      )
      
      Page(response.tookInMillis, response.totalHits, args.offset, args.limit, items, aggregations)
    }
  }

}
