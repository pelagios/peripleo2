package services.item.search;

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ HitAs, QueryDefinition, RangeQueryDefinition }
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.reflectiveCalls
import services.ES
import services.item.Item

class SearchFilter(args: SearchArgs, placeFilterDefinition: Option[QueryDefinition] = None) {
	
	private def build(includeDateRangeFilter: Boolean) = {
	  val dateRangeClauses = 
      if (includeDateRangeFilter) args.filters.dateRangeFilter.map(_.filterDefinition)
      else None
      
    val filterClauses =
      Seq(
        args.filters.itemTypeFilter.map(_.filterDefinition("item_type")),
        args.filters.categoryFilter.map(_.filterDefinition("is_conflation_of.category")),
        args.filters.datasetFilter.map(_.filterDefinition("is_conflation_of.is_in_dataset.ids")),
        args.filters.languageFilter.map(_.filterDefinition("is_conflation_of.languages")),
        args.filters.spatialFilter.map(_.filterDefinition),
        placeFilterDefinition,
        { if (args.filters.hasDepiction.getOrElse(false)) Some(existsQuery("is_conflation_of.depictions.url")) else None }
      ).flatten ++ dateRangeClauses.getOrElse(Seq.empty[RangeQueryDefinition])

    val notClauses = Seq(
      { if (!args.filters.hasDepiction.getOrElse(true)) Some(existsQuery("is_conflation_of.depictions.url")) else None },
      { if (args.filters.rootOnly) Some(existsQuery("is_part_of")) else None  }
    ).flatten 

    if (notClauses.size > 0)
      bool { must(filterClauses) not (notClauses) }
    else
      bool { must(filterClauses) }
  }
	
	lazy val withDateRangeFilter = build(true)
	
	lazy val withoutDateRangeFilter = build(false)

}

object SearchFilter {

	/** Different handling required depending on whether there's a place filter or not **/
	def build(args: SearchArgs)(implicit es: ES, ctx: ExecutionContext, hitAs: HitAs[Item]) = args.filters.placeFilter match {
    case Some(placeFilter) => placeFilter.filterDefinition().map(f => new SearchFilter(args, Some(f)))
    case None => Future.successful(new SearchFilter(args))
  }
  
}
