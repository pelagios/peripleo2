package services.item.search.filters

case class TermFilter(values: Seq[String], setting: TermFilter.Setting)

object TermFilter {
  
  sealed trait Setting  
  case object EXCLUDE extends Setting
  case object ONLY extends Setting
  
}