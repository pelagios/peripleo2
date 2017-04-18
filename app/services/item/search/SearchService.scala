package services.item.search

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ AbstractAggregationDefinition, HitAs, RangeQueryDefinition, RichSearchHit }
import javax.inject.{ Inject, Singleton }
import org.joda.time.{ DateTime, DateTimeZone }
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.Json
import scala.concurrent.{ Future, ExecutionContext }
import scala.language.reflectiveCalls
import services.{ ES, Page }
import services.item.{ Item, ItemService }
import org.elasticsearch.search.aggregations.bucket.histogram.InternalHistogram
import services.item.search.filters.TermFilter
import services.notification.NotificationService

@Singleton
class SearchService @Inject() (
  implicit val es: ES,
  implicit val notifications: NotificationService,
  implicit val ctx: ExecutionContext
) {

  private val dateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd").withZone(DateTimeZone.UTC)

  private def formatDate(dt: DateTime) = dateFormatter.print(dt.withZone(DateTimeZone.UTC))

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

  private def buildFilters(args: SearchArgs) = {

    def termFilterDefinition(field: String, filter: TermFilter) = {
      val filters = filter.values.map(termQuery(field, _))
      filter.setting match {
        case TermFilter.ONLY => should(filters)
        case TermFilter.EXCLUDE => not(filters)
      }
    }

    def timerangeFilterDefinition() = args.filters.dateRangeFilter.map { filter =>
      (filter.from, filter.to) match {
        case (Some(from), Some(to)) =>
          Seq(
            rangeQuery("temporal_bounds.from").to(formatDate(to)),
            rangeQuery("temporal_bounds.to").from(formatDate(from))
          )

        case _ =>
          Seq()
          // TODO support open intervals
      }
    }.getOrElse(Seq.empty[RangeQueryDefinition])

    val filterClauses =
      Seq(
        args.filters.itemTypeFilter.map(termFilterDefinition("item_type", _)),
        args.filters.categoryFilter.map(termFilterDefinition("is_conflation_of.category", _)),
        args.filters.datasetFilter.map(termFilterDefinition("is_conflation_of.is_in_dataset", _)),
        args.filters.languageFilter.map(termFilterDefinition("is_conflation_of.languages", _)),
        // Check for existing 'depictions' field iff hasDepictions is set to true
        { if (args.filters.hasDepiction.getOrElse(false)) Some(nestedQuery("depictions") query { existsQuery("depictions.url") }) else None }
      ).flatten ++ timerangeFilterDefinition()

    val notClauses = Seq(
      // Check for missing 'depicitions' field iff hasDepicitions is set to false
      { if (!args.filters.hasDepiction.getOrElse(true)) Some(nestedQuery("depictions") query { existsQuery("depictions.url") }) else None },
      { if (args.filters.rootOnly) Some(existsQuery("is_part_of")) else None  }
    ).flatten

    if (notClauses.size > 0)
      bool { must(filterClauses) not (notClauses) }
    else
      bool { must(filterClauses) }
  }

  private def buildAggregationDefinitions(args: SearchArgs) = {
    val termAggregations =
      if (args.settings.termAggregations)
        Seq(
          aggregation terms "by_type" field "item_type" size 20,
          aggregation terms "by_dataset" field "is_conflation_of.is_in_dataset" size 20,
          aggregation terms "by_language" field "is_conflation_of.languages" size 20)
      else
        Seq.empty[AbstractAggregationDefinition]

    val timeHistogramAggregations =
      if (args.settings.timeHistogram)
        Seq(
          aggregation histogram "by_decade" script histogramScript(10) interval 10,
          aggregation histogram "by_century" script histogramScript(100) interval 100)
      else
        Seq.empty[AbstractAggregationDefinition]

    termAggregations ++ timeHistogramAggregations
  }

  private def buildItemQuery(args: SearchArgs) =
    search in ES.PERIPLEO / ES.ITEM query {
      bool {
        must (
          should (
            queryStringQuery(args.query.getOrElse("*")),
            hasChildQuery("reference") query { termQuery("context", args.query.getOrElse("*")) }
          )
        ) // filter(buildFilters(args))
      }
    // TODO need to figure out how to design the query
    // TODO we want post-filtering for the time histogram, but not the other aggregations
    } postFilter buildFilters(args) start args.offset limit args.limit aggregations buildAggregationDefinitions(args)

  private def buildPlaceQuery(args: SearchArgs) =
    search in ES.PERIPLEO / ES.REFERENCE query {
      bool {
        must (
          should (
            termQuery("context", args.query.getOrElse("*")),
            hasParentQuery(ES.ITEM) query { queryStringQuery(args.query.getOrElse("*")) }
          )
        ) filter {
          bool {
            must(
              hasParentQuery("item") query buildFilters(args)
            )
          }
        }
      }
    } start 0 limit 0 aggregations (
      aggregation
        terms "by_place"
        field "reference_to.doc_id"
        size 600
    ) // TODO sub-aggregate places vs people vs periods etc. to places only

  def query(args: SearchArgs): Future[RichResultPage] = {
    val startTime = System.currentTimeMillis

    val fPlaceQuery = es.client execute {
      buildPlaceQuery(args)
    } map { response =>
      Aggregation.parseTerms(response.aggregations.get("by_place")).buckets
    }

    val fItemQuery = es.client execute {
      buildItemQuery(args)
    } map { response =>
      val items = response.as[Item].toSeq
      val aggregations = {
        Option(response.aggregations) match {
          case Some(aggs) =>
            val histogram =
              (Option(aggs.get("by_century")), Option(aggs.get("by_decade"))) match {
                case (Some(_), Some(_)) =>
                  val byCentury = Aggregation.parseHistogram(aggs.get("by_century"), "by_time")
                  val byDecade = Aggregation.parseHistogram(aggs.get("by_decade"), "by_time")
                  if (byCentury.buckets.size >= 40) Some(byCentury) else Some(byDecade)

                case _ => None
            }

            Seq(
              Option(response.aggregations.get("by_type")).map(Aggregation.parseTerms),
              Option(response.aggregations.get("by_dataset")).map(Aggregation.parseTerms),
              Option(response.aggregations.get("by_language")).map(Aggregation.parseTerms),
              histogram
            ).flatten

          case None => Seq.empty[Aggregation]
        }
      }

      (response.totalHits, items, aggregations)
    }

    if (args.settings.topPlaces) {
      val fResults = for {
        (totalHits, items, aggregations) <- fItemQuery
        placeCounts <- fPlaceQuery
        places <- ItemService.resolveItems(placeCounts.map(_._1))
      } yield (totalHits, items, aggregations, placeCounts, places)

      fResults.map { case (totalHits, items, aggregations, placeCounts, places) =>
        val topPlaces = TopPlaces.build(placeCounts, places)
        RichResultPage(System.currentTimeMillis - startTime, totalHits, args.offset, args.limit, items, aggregations, Some(topPlaces))
      }
    } else {
      fItemQuery.map { case (totalHits, items, aggregations) =>
        RichResultPage(System.currentTimeMillis - startTime, totalHits, args.offset, args.limit, items, aggregations, None)
      }
    }
  }

}
