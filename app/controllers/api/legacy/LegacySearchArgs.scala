package controllers.api.legacy

import services.item.search.filters._
import services.item.search.{ResponseSettings, SearchArgs, SearchArgsParser, SearchFilters}

object LegacySearchArgs extends SearchArgsParser {
  
  private val TYPE_CORRESPONDENCE = Map(
    "place"   -> "PLACE",
    "dataset" -> "DATASET",
    "item"    -> "OBJECT")
  
  /** Builds term filters according to the new API.
    *
    * Optionally takes a Map[String, String] that is used
    * to 'translate' the term filter values, so that we can 
    * translate between legacy terms (e.g. item --> OBJECT)   
    */
  def buildIncludesTermFilter(key: String, q: Map[String, Seq[String]], translation: Option[Map[String, String]]) = { 
    val includes = getArg(key, q)
    includes match {        
      case Some(in) =>
        translation match {
          case Some(table) =>
            val translated = split(in).map(v => table.get(v).getOrElse(v))
            Some(TermFilter(translated, TermFilter.ONLY))   
           
          case None => 
            Some(TermFilter(split(in), TermFilter.ONLY))
        }
        
        
      case _ => None
    }
  }
  
  def fromQueryString(q: Map[String, Seq[String]]) = {
    val filters = SearchFilters(
      buildIncludesTermFilter("types", q, Some(TYPE_CORRESPONDENCE)),
      None, // categories
      buildIncludesTermFilter("datasets", q, None),
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