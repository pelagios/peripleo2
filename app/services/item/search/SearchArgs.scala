package services.item.search

import org.joda.time.format.{ DateTimeFormat, DateTimeFormatterBuilder }
import services.item.search.filters._
import org.joda.time.format.DateTimePrinter
import com.vividsolutions.jts.geom.Coordinate

case class SearchArgs(
    
  query: Option[String],
  
  limit: Int,
  
  offset: Int,
  
  filters: SearchFilters,
  
  settings: ResponseSettings
  
)

case class SearchFilters(
    
  itemTypeFilter: Option[TermFilter],
  
  categoryFilter: Option[TermFilter],
  
  datasetFilter: Option[TermFilter],
  
  languageFilter: Option[TermFilter],
  
  placeFilter: Option[PlaceFilter],
  
  dateRangeFilter: Option[DateRangeFilter],
  
  spatialFilter: Option[SpatialFilter],
  
  hasDepiction: Option[Boolean],
  
  rootOnly: Boolean // Exclude items that are part of another item
  
)

case class ResponseSettings(

  timeHistogram: Boolean,
  
  termAggregations: Boolean,
  
  topPlaces: Boolean
    
)
  
object SearchArgs {
  
  private val dateFormatter = new DateTimeFormatterBuilder().append(null, Array(
    DateTimeFormat.forPattern("yyyy-MM-dd").getParser,
    DateTimeFormat.forPattern("yyyy-MM").getParser,
    DateTimeFormat.forPattern("yyyy").getParser
  )).toFormatter()
  
  private def getArg(key: String, queryString: Map[String, Seq[String]]): Option[String] = 
    queryString
      .filter(_._1.equalsIgnoreCase(key))
      .headOption.flatMap(_._2.headOption)
      
  private def split(str: String) = str.split(",").map(_.trim).toSeq
      
  private def buildTermFilter(includeKey: String, excludeKey: String, queryString: Map[String, Seq[String]]): Option[TermFilter] = {
    val includes = getArg(includeKey, queryString)
    val excludes = getArg(excludeKey, queryString)
    
    (includes, excludes) match {        
      case (Some(in), None) =>
        Some(TermFilter(split(in), TermFilter.ONLY))

      case (None, Some(ex)) =>
        Some(TermFilter(split(ex), TermFilter.EXCLUDE))
        
      case (Some(in), Some(ex)) =>
        // Wait what? By the throw of a coin, we choose include to get priority
        Some(TermFilter(split(in), TermFilter.ONLY))
        
      case _ => None
    }
  }
  
  private def buildDateRangeFilter(queryString: Map[String, Seq[String]]): Option[DateRangeFilter] = {
    val from = getArg("from", queryString).map(date => dateFormatter.parseDateTime(date))
    val to = getArg("to", queryString).map(date => dateFormatter.parseDateTime(date))
    val filterMode = getArg("date_filter", queryString)
      .map { _.toLowerCase match {      
        case "contain" => DateRangeFilter.CONTAIN
        case _ => DateRangeFilter.INTERSECT      
      }}.getOrElse(DateRangeFilter.INTERSECT)
    
    // Use a filter if at least one arg is defined
    if (from.isDefined || to.isDefined)
      Some(DateRangeFilter(from, to, filterMode))
    else
      None
  }
  
  private def buildSpatialFilter(q: Map[String, Seq[String]]): Option[SpatialFilter] = {
    val bbox = getArg("bbox", q).map(BoundingBox.fromString(_))
    val center = (getArg("lon", q), getArg("lat", q)) match {
      case (Some(lon), Some(lat)) => Some(new Coordinate(lon.toDouble, lat.toDouble))   
      // Throw exception if only one arg is defined, but not the other
      case (Some(_), _) | (_, Some(_)) => throw new IllegalArgumentException
      case _ => None
    }
    
    if (bbox.isDefined || center.isDefined)
      Some(SpatialFilter(bbox, center, getArg("radius", q).map(_.toDouble)))
    else
      None
  }
  
  def fromQueryString(q: Map[String, Seq[String]]) = {
    val filters = SearchFilters(
      buildTermFilter("types", "ex_types", q),
      buildTermFilter("categories", "ex_categories", q),
      buildTermFilter("datasets", "ex_datasets", q),
      buildTermFilter("langs", "ex_langs", q),
      getArg("places", q).map(uris => PlaceFilter(split(uris), TermFilter.ONLY)),
      buildDateRangeFilter(q),
      buildSpatialFilter(q),
      getArg("has_images", q).map(_.toBoolean),
      getArg("root_only", q).map(_.toBoolean).getOrElse(false)
    )
    
    val settings = ResponseSettings( 
      getArg("time_histogram", q).map(_.toBoolean).getOrElse(false),
      getArg("facets", q).map(_.toBoolean).getOrElse(false),
      getArg("top_places", q).map(_.toBoolean).getOrElse(false)
    )
   
    SearchArgs(
      getArg("q", q),
      getArg("limit", q).map(_.toInt).getOrElse(20),
      getArg("offset", q).map(_.toInt).getOrElse(0),
      filters,
      settings
    )
  }

}