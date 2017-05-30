package services.item.search

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import javax.inject.{ Inject, Singleton }
import play.api.libs.json.Json
import scala.concurrent.{ Future, ExecutionContext }
import scala.language.reflectiveCalls
import services.{ ES, Page }
import services.item.{ Item, ItemService }
import org.elasticsearch.script.ScriptService.ScriptType
import org.elasticsearch.search.aggregations.bucket.histogram.InternalHistogram
import services.item.search.filters.TermFilter
import services.notification.NotificationService

@Singleton
class SearchService @Inject() (
  implicit val es: ES,
  implicit val notifications: NotificationService,
  implicit val ctx: ExecutionContext
) {

  implicit object ItemHitAs extends HitAs[Item] {
    override def as(hit: RichSearchHit): Item =
      Json.fromJson[Item](Json.parse(hit.sourceAsString)).get
  }
  
  /** The common phrase query part **/
  private def customPhraseQuery(q: String) =
    Seq(
        // Treat as standard query string query first...
        queryStringQuery(q).defaultOperator("AND"),

        // ...and then look for exact matches in specific fields
        bool {
          should (
            // Search inside record titles...
            matchPhraseQuery("is_conflation_of.title.raw", q).boost(5.0),
            matchPhraseQuery("is_conflation_of.title", q),

            // ...names...
            matchPhraseQuery("is_conflation_of.names.name.raw", q).boost(5.0),
            matchPhraseQuery("is_conflation_of.names.name", q),

            // ...and descriptions (with lower boost)
            matchQuery("is_conflation_of.descriptions.description", q).operator("AND")
          )
        }
      )

  private def buildPlaceQuery(args: SearchArgs, filter: QueryDefinition) =
    search in ES.PERIPLEO / ES.REFERENCE query {
      constantScoreQuery {
        should (
          termQuery("context", args.query.getOrElse("*")),
          hasParentQuery(ES.ITEM) query { queryStringQuery(args.query.getOrElse("*")) }
        ) filter (
          hasParentQuery("item") query filter
        )
      }
    } start 0 limit 0 aggregations (
      aggregation
        terms "by_place"
        field "reference_to.doc_id"
        size 600,
        
      aggregation
        terms "by_relation"
        field "relation"
        size 10
    ) // TODO sub-aggregate places vs people vs periods etc. to places only
  
  /** Common query components used to build item result and time histogram **/
  private def itemBaseQuery(args: SearchArgs, filter: QueryDefinition) = { 
    val itemPart = args.query.map(customPhraseQuery).getOrElse(Seq(queryStringQuery("*")))
    val referencePart = hasChildQuery("reference") query { termQuery("context", args.query.getOrElse("*")) }
      
    search in ES.PERIPLEO / ES.ITEM query {
      bool {
        must ( should ( itemPart :+ referencePart ) ) filter(filter)
      } 
    }
  }

  private def buildItemQuery(args: SearchArgs, filter: QueryDefinition) = {
    val aggregations =
      if (args.settings.termAggregations)
        Seq(
          aggregation terms "by_type" field "item_type" size 20,
          aggregation terms "by_dataset" field "is_conflation_of.is_in_dataset.paths" size 20,
          aggregation terms "by_language" field "is_conflation_of.languages" size 20)
      else
        Seq.empty[AbstractAggregationDefinition]
    
    itemBaseQuery(args, filter) start args.offset limit args.limit aggregations aggregations
  }   
 
  private def buildTimeHistogramQuery(args: SearchArgs, filter: QueryDefinition) =
    itemBaseQuery(args, filter) limit 0 aggregations Seq(
      aggregation histogram "by_decade"  script { script("by_time") params(Map("interval" -> 10))  scriptType ScriptType.FILE } interval 10,
      aggregation histogram "by_century" script { script("by_time").params(Map("interval" -> 100)) scriptType ScriptType.FILE } interval 100)

  def query(args: SearchArgs): Future[RichResultPage] = {

    val startTime = System.currentTimeMillis
    
    // Building filters is an async process as some may require expansion
    SearchFilter.build(args).flatMap { filter =>
      
      val fPlaceQuery =
        es.client execute { buildPlaceQuery(args, filter.withDateRangeFilter) } map { response =>
          // TODO build by-relation aggregation (it's already included in the response)
          Aggregation.parseTerms(response.aggregations.get("by_place")).buckets
        }

      val fItemQuery = es.client execute {
        buildItemQuery(args, filter.withDateRangeFilter)
      } map { response =>
        val items = response.as[Item].toSeq
        val aggregations =
          Option(response.aggregations) match {
            case Some(aggs) =>  
              Seq(
                Option(response.aggregations.get("by_type")).map(Aggregation.parseTerms),
                Option(response.aggregations.get("by_dataset")).map(Aggregation.parseTerms),
                Option(response.aggregations.get("by_language")).map(Aggregation.parseTerms)
              ).flatten
  
            case None => Seq.empty[Aggregation]
          }
  
        (response.totalHits, items, aggregations)
      }
      
      val fHistogramQuery =
        if (args.settings.timeHistogram)
          es.client execute { buildTimeHistogramQuery(args, filter.withoutDateRangeFilter) } map { response =>
            val byCentury = Aggregation.parseHistogram(response.aggregations.get("by_century"), "by_time")
            val byDecade = Aggregation.parseHistogram(response.aggregations.get("by_decade"), "by_time")
            if (byCentury.buckets.size >= 40) Some(byCentury) else Some(byDecade)
          }
        else
          Future.successful(None)
      
      if (args.settings.topPlaces) {
        val fResults = for {
          (totalHits, items, aggregations) <- fItemQuery
          placeCounts <- fPlaceQuery
          histogram <- fHistogramQuery
          places <- ItemService.resolveItems(placeCounts.map(_._1))
        } yield (totalHits, items, aggregations, placeCounts, histogram, places)
  
        fResults.map { case (totalHits, items, aggregations, placeCounts, histogram, places) =>
          val topPlaces = TopPlaces.build(placeCounts, places)
          val aggs = histogram match {
            case Some(h) => aggregations :+ h
            case None => aggregations
          }
          
          RichResultPage(System.currentTimeMillis - startTime, totalHits, args.offset, args.limit, items, aggs, Some(topPlaces))
        }
      } else {
        fItemQuery.map { case (totalHits, items, aggregations) =>
          RichResultPage(System.currentTimeMillis - startTime, totalHits, args.offset, args.limit, items, aggregations, None)
        }
      }
      
    }
  }

}
