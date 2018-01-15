package services.item.search

import com.sksamuel.elastic4s.{Hit, HitReader}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.searches.queries.QueryDefinition
import es.ES
import javax.inject.{ Inject, Singleton }
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import play.api.libs.json.Json
import scala.concurrent.{ Future, ExecutionContext }
import scala.language.reflectiveCalls
import services.{ Aggregation, Page }
import services.item.{ Item, ItemService }
import services.item.reference.TopReferenced
import services.notification.NotificationService
import org.apache.lucene.search.join.ScoreMode
import org.elasticsearch.script.ScriptType
import com.sksamuel.elastic4s.searches.RichSearchResponse

@Singleton
class SearchService @Inject() (
  implicit val es: ES,
  implicit val notifications: NotificationService,
  implicit val ctx: ExecutionContext
) extends HasTimerangeQuery {

  private def toRichResultItem(response: RichSearchResponse): Seq[RichResultItem] =
    response.hits.map { hit =>
      val item = Json.fromJson[Item](Json.parse(hit.sourceAsString)).get
      val isHitOnReference = !hit.matchedQueries.contains("item_meta_match")
      RichResultItem(item, hit.score, isHitOnReference)
    }

  /** The common phrase query part **/
  private def phraseQuery(query: Option[String]): Seq[QueryDefinition] = {
    query.map { q =>
      Seq(
        queryStringQuery(q)
          .field("is_conflation_of.title")
          .field("is_conflation_of.names.name")
          .field("is_conflation_of.descriptions.description")
          .defaultOperator("AND").queryName("item_meta_match"),

        // Search inside record titles...
        matchPhraseQuery("is_conflation_of.title.raw", q).boost(5.0),
        matchPhraseQuery("is_conflation_of.title", q),

        // ...names...
        matchPhraseQuery("is_conflation_of.names.name.raw", q).boost(5.0),
        matchPhraseQuery("is_conflation_of.names.name", q),

        // ...and descriptions (with lower boost)
        matchQuery("is_conflation_of.descriptions.description", q))
    }.getOrElse(Seq(matchAllQuery))
  }

  private def buildTopRelatedQuery(args: SearchArgs, filter: QueryDefinition) =
    search(ES.PERIPLEO / ES.REFERENCE) query {
      constantScoreQuery {
        boolQuery
          must (
            should (
              {
                queryStringQuery(args.query.getOrElse("*")).field("quote.context") +:
                phraseQuery(args.query).map { q => hasParentQuery(ES.ITEM) query { q } scoreMode false }
              }
            )
          ) filter (
            hasParentQuery("item") query filter scoreMode false
          )
      }
    } start 0 limit 0 aggregations (
      // Aggregate by reference type (PLACE | PERSON | PERIOD)
      termsAggregation("by_related") field "reference_to.item_type" size 10 subaggs (
        // Sub-aggregate by docId
        termsAggregation("by_doc_id") field "reference_to.doc_id" size ES.MAX_SIZE subaggs (
          // Sub-sub-aggregate by relation
          termsAggregation("by_relation") field "relation" size 10
        )
      ),

      // Aggregate by relation at top level
      termsAggregation("by_relation") field "relation" size 10
    )

  /** Common query components used to build item result and time histogram **/
  private[search] def itemBaseQuery(args: SearchArgs, filter: QueryDefinition) =
    search(ES.PERIPLEO / ES.ITEM) query {
      boolQuery
        must (
          should (
            phraseQuery(args.query) ++

            // If there is a query, search reference contexts
            args.query
              .map { q => Seq(hasChildQuery(ES.REFERENCE) query { queryStringQuery(q).field("quote.context") } scoreMode(ScoreMode.Avg) queryName("ref_context_match")) }
              .getOrElse(Seq.empty[QueryDefinition])
          )
        ) filter (filter)
    }

  private def buildItemQuery(args: SearchArgs, filter: QueryDefinition) = {
    val aggregations =
      if (args.settings.termAggregations)
        Seq(
          termsAggregation("by_type") field "item_type" size 20,
          termsAggregation("by_dataset") field "is_conflation_of.is_in_dataset.paths" size 20,
          termsAggregation("by_language") field "is_conflation_of.languages" size 20)
      else
        Seq()

    itemBaseQuery(args, filter) start args.offset limit args.limit aggregations aggregations
  }

  private def buildTimeHistogramQuery(args: SearchArgs, filter: QueryDefinition) =
    itemBaseQuery(args, filter) limit 0 aggregations Seq(
      dateHistogramAggregation("by_decade") script { script("by_time") params(Map("interval" -> 10))  scriptType ScriptType.FILE lang "groovy" } interval 10,
      dateHistogramAggregation("by_century") script { script("by_time") params(Map("interval" -> 100)) scriptType ScriptType.FILE lang "groovy" } interval 100)
  
  def query(args: SearchArgs): Future[RichResultPage] = {

    val startTime = System.currentTimeMillis

    // Building filters is an async process as some may require expansion
    SearchFilter.build(args).flatMap { filter =>
      val fTopReferencedQuery =
        es.client execute { buildTopRelatedQuery(args, filter.withDateRangeFilter) } map { response =>
          val topReferenced = TopReferenced.parseAggregation(response.aggregations)
          val byRelation = Aggregation.parseTerms(response.aggregations.termsResult("by_relation")).buckets
          (topReferenced, byRelation)
        }

      val fItemQuery = es.client execute {
        buildItemQuery(args, filter.withDateRangeFilter)
      } map { response =>
        val items = toRichResultItem(response)
        val aggregations =
          Option(response.aggregations) match {
            case Some(aggs) =>
              Seq(
                Option(response.aggregations.termsResult("by_type")).map(Aggregation.parseTerms),
                Option(response.aggregations.termsResult("by_dataset")).map(Aggregation.parseTerms),
                Option(response.aggregations.termsResult("by_language")).map(Aggregation.parseTerms)
              ).flatten

            case None => Seq.empty[Aggregation]
          }

        (response.totalHits, items, aggregations)
      }

      val fHistogramQuery =
        if (args.settings.timeHistogram)
          es.client execute { buildTimeHistogramQuery(args, filter.withoutDateRangeFilter) } map { response =>
            val byCentury = Aggregation.parseHistogram(response.aggregations.histogramResult("by_century"), "by_time")
            val byDecade = Aggregation.parseHistogram(response.aggregations.histogramResult("by_decade"), "by_time")
            if (byCentury.buckets.size >= 40) Some(byCentury) else Some(byDecade)
          }
       else
          Future.successful(None)

      if (args.settings.topReferenced) {
        val fResults = for {
          (totalHits, items, aggregations) <- fItemQuery
          (unresolvedTopReferenced, byRelation) <- fTopReferencedQuery
          histogram <- fHistogramQuery
          topReferenced <- unresolvedTopReferenced.resolve()
        } yield (totalHits, items, aggregations, byRelation, histogram, topReferenced)

        fResults.map { case (totalHits, items, aggregations, byRelation, histogram, topReferenced) =>
          val aggs = histogram match {
            case Some(h) => aggregations :+ h
            case None => aggregations
          }

          RichResultPage(System.currentTimeMillis - startTime, totalHits, args.offset, args.limit, items, aggs, Some(topReferenced))
        }
      } else {
        val fResults = for {
          (totalHits, items, aggregations) <- fItemQuery
          histogram <- fHistogramQuery
        } yield (totalHits, items, aggregations, histogram)

        fResults.map { case (totalHits, items, aggregations, histogram) =>
          val aggs = histogram match {
            case Some(h) => aggregations :+ h
            case None => aggregations
          }
          RichResultPage(System.currentTimeMillis - startTime, totalHits, args.offset, args.limit, items, aggs, None)
        }
      }

    }
  }

}
