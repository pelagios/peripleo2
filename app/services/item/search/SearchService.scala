package services.item.search

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ HitAs, RichSearchHit }
import javax.inject.{ Inject, Singleton }
import play.api.libs.json.Json
import scala.concurrent.{ Future, ExecutionContext }
import services.{ ES, Page }
import services.item.Item
import org.elasticsearch.search.aggregations.bucket.histogram.InternalHistogram
import services.item.place.PlaceService

@Singleton
class SearchService @Inject() (val es: ES, placeService: PlaceService, implicit val ctx: ExecutionContext) {
  
  private def histogramScript(interval: Int) =
    s"""
     f = doc['temporal_bounds.from']
     t = doc['temporal_bounds.to']
     buckets = []
     if (!(f.empty || t.empty))
       for (i=f.date.year; i<t.date.year; i+= $interval) { buckets.add(i) }
     buckets;
     """
       
  private def buildItemQuery(args: SearchArgs) =
    search in ES.PERIPLEO / ES.ITEM query {
      bool {
        should(
          queryStringQuery(args.query.getOrElse("*")),
          hasChildQuery("reference") query { termQuery("context", args.query.getOrElse("*")) }
        )
      }
    } start args.offset limit args.limit aggregations (
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
  
  private def buildPlaceQuery(args: SearchArgs) =
    search in ES.PERIPLEO / ES.REFERENCE query {
      bool {
        should(
          termQuery("context", args.query.getOrElse("*")),
          hasParentQuery("item") query { queryStringQuery(args.query.getOrElse("*")) }
        )
      }
    } start 0 limit 0 aggregations (
      aggregation
        terms "by_place"
        field "uri"
        size 600  
    ) // TODO sub-aggregate places vs people vs periods etc. to places only

  implicit object ItemHitAs extends HitAs[Item] {
    override def as(hit: RichSearchHit): Item =
      Json.fromJson[Item](Json.parse(hit.sourceAsString)).get
  }

  def query(args: SearchArgs): Future[Page[Item]] = {
    
    val startTime = System.currentTimeMillis
    
    val fPlaceQuery = es.client execute {
      buildPlaceQuery(args)
    } map { response =>
      Aggregation.parseTerms(response.aggregations.get("by_place"))
    }
    
    val fItemQuery = es.client execute {
      buildItemQuery(args)
    } map { response =>
      val items = response.as[Item].toSeq

      val byCentury = Aggregation.parseHistogram(response.aggregations.get("by_century"), "by_time")
      val byDecade = Aggregation.parseHistogram(response.aggregations.get("by_decade"), "by_time")
      
      val aggregations = Seq(
        Aggregation.parseTerms(response.aggregations.get("by_type")),
        Aggregation.parseTerms(response.aggregations.get("by_language")),
        if (byCentury.buckets.size >= 20) byCentury else byDecade
      )
      
      (response.totalHits, items, aggregations)
    }
    
    val fResults = for {
      (totalHits, items, aggregations) <- fItemQuery
      topPlaceCounts <- fPlaceQuery
      topPlaces <- placeService.findByRootUris(topPlaceCounts.buckets.map(_._1))
    } yield (totalHits, items, aggregations, topPlaceCounts, topPlaces)
    
    fResults.map { case (totalHits, items, aggregations, topPlaceCounts, topPlaces) =>
      // TODO format top places
      Page(System.currentTimeMillis - startTime, totalHits, args.offset, args.limit, items, aggregations)
    }
  }

}
