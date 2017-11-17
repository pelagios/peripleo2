package controllers.api.legacy

import services.item.search.filters._
import services.item.search.{ResponseSettings, SearchArgs, SearchArgsParser, SearchFilters}

object LegacySearchArgs extends SearchArgsParser {
  
  def fromQueryString(q: Map[String, Seq[String]]) = {
    // val termFilter = buildTermFilter("types", "ex_types", q).map(
    
    
    
    val filters = SearchFilters(
      buildTermFilter("types", "ex_types", q),
      None, // categories
      buildTermFilter("datasets", "ex_datasets", q),
      None, // langs
      getArg("places", q).map(uris => ReferencedItemFilter(split(uris), TermFilter.ONLY)),
      buildDateRangeFilter(q),
      buildSpatialFilter(q),
      None, // has depiction
      false // root only
    )

    SearchArgs(
      getArg("query", q),
      getArg("limit", q).map(_.toInt).getOrElse(20),
      getArg("offset", q).map(_.toInt).getOrElse(0),
      filters,
      ResponseSettings.DEFAULT
    )
  }
  
}