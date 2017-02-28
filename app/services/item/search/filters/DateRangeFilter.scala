package services.item.search.filters

import org.joda.time.DateTime

case class DateRangeFilter(from: Option[DateTime], to: Option[DateTime], setting: DateRangeFilter.Setting) {
  
  require(from.isDefined || to.isDefined)
  
}

object DateRangeFilter {
  
  sealed trait Setting  
  case object INTERSECT extends Setting
  case object CONTAIN extends Setting
  
}