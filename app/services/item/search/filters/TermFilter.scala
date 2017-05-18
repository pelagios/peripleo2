package services.item.search.filters

import com.sksamuel.elastic4s.ElasticDsl._

case class TermFilter(values: Seq[String], setting: TermFilter.Setting) {
  
  /** Builds the ES filter definition for this filter **/
  def filterDefinition(field: String) = {
    val filters = values.map(termQuery(field, _))
    setting match {
      case TermFilter.ONLY => should(filters)
      case TermFilter.EXCLUDE => not(filters)
    }
  }
  
}

object TermFilter {
  
  sealed trait Setting  
  case object EXCLUDE extends Setting
  case object ONLY extends Setting

}