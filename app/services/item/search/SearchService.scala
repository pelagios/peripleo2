package services.item.search

import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.ElasticDsl._
import javax.inject.{ Inject, Singleton }
import org.elasticsearch.script.ScriptService.ScriptType
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import play.api.libs.json.Json
import scala.concurrent.{ Future, ExecutionContext }
import scala.language.reflectiveCalls
import services.{ ES, Page }
import services.item.{ Item, ItemService }
import services.item.reference.TopReferenced
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
  private def phraseQuery(query: Option[String]): Seq[QueryDefinition] = {
    query.map { q =>
      Seq(
        queryStringQuery(q).field("is_conflation_of.title").field("is_conflation_of.descriptions.description").defaultOperator("AND"),

        // Search inside record titles...
        matchPhraseQuery("is_conflation_of.title.raw", q).boost(5.0),
        matchPhraseQuery("is_conflation_of.title", q),

        // ...names...
        matchPhraseQuery("is_conflation_of.names.name.raw", q).boost(5.0),
        matchPhraseQuery("is_conflation_of.names.name", q),

        // ...and descriptions (with lower boost)
        matchQuery("is_conflation_of.descriptions.description", q).operator("AND"))
    }.getOrElse(Seq(matchAllQuery))
  }

  private def buildTopRelatedQuery(args: SearchArgs, filter: QueryDefinition) =
    search in ES.PERIPLEO / ES.REFERENCE query {
      constantScoreQuery {
        bool {
          must (
            should (
              queryStringQuery(args.query.getOrElse("*")).field("context") +:
              phraseQuery(args.query).map { q => hasParentQuery(ES.ITEM) query { q } }
            )
          ) filter (
            hasParentQuery("item") query filter
          )
        }
      }
    } start 0 limit 0 aggregations (
      // Aggregate by reference type (PLACE | PERSON | PERIOD)
      aggregation terms "by_related" field "reference_type" size 10 aggregations (
        // Sub-aggregate by docId
        aggregation terms "by_doc_id" field "reference_to.doc_id" size ES.MAX_SIZE aggregations (
          // Sub-sub-aggregate by relation
          aggregation terms "by_relation" field "relation" size 10
        )
      ),

      // Aggregate by relation at top level
      aggregation terms "by_relation" field "relation" size 10
    )

  /** Common query components used to build item result and time histogram **/
  private def itemBaseQuery(args: SearchArgs, filter: QueryDefinition) =
    search in ES.PERIPLEO / ES.ITEM query {
      bool {
        must (
          should (
            phraseQuery(args.query) ++

            // If there is a query, search reference contexts
            args.query
              .map { q => Seq(hasChildQuery(ES.REFERENCE) query { queryStringQuery(q).field("context") }) }
              .getOrElse(Seq.empty[QueryDefinition])
          )
        ) filter (filter)
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

      val fTopReferencedQuery =
        es.client execute { buildTopRelatedQuery(args, filter.withDateRangeFilter) } map { response =>
          val topReferenced = TopReferenced.parseAggregation(response.aggregations)
          val byRelation = Aggregation.parseTerms(response.aggregations.get("by_relation")).buckets
          (topReferenced, byRelation)
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
