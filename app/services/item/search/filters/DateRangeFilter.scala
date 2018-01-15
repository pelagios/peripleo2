package services.item.search.filters

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.searches.queries.RangeQueryDefinition
import org.joda.time.{ DateTime, DateTimeZone }
import org.joda.time.format.DateTimeFormat

case class DateRangeFilter(from: Option[DateTime], to: Option[DateTime], setting: DateRangeFilter.Setting) {
  
  require(from.isDefined || to.isDefined)
  
  private def formatDate(dt: DateTime) = DateRangeFilter.dateFormatter.print(dt.withZone(DateTimeZone.UTC))
  
  /** Builds the ES filter definition for this filter **/
  def filterDefinition() = (from, to) match {
    case (Some(from), Some(to)) =>
      Seq(
        rangeQuery("temporal_bounds.from").to(formatDate(to)),
        rangeQuery("temporal_bounds.to").from(formatDate(from))
      )
      
      // TODO support open intervals
      case _ => Seq.empty[RangeQueryDefinition]
    }
  
}

object DateRangeFilter {
  
  sealed trait Setting  
  case object INTERSECT extends Setting
  case object CONTAIN extends Setting
  
  private[filters] val dateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd").withZone(DateTimeZone.UTC)
  
}