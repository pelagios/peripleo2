package services.item.search

import com.vividsolutions.jts.geom.Coordinate
import org.joda.time.DateTime
import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import services.item.search.filters._

class SearchArgsSpec extends PlaySpec {
  
   "A valid set of query parameters" should {
     
     "be parsed correctly" in {
       
       val validParams = Map(
         "q" -> Seq("athens"),
         "limit" -> Seq("200"),
         "offset" -> Seq("10"),
         "from" -> Seq("-330"),
         "to" -> Seq("200"),
         "places" -> Seq("http://pleiades.stoa.org/places/579885"),
         "langs" -> Seq("la,de"),
         "ex_categories" -> Seq("archaeology"),
         "top_related" -> Seq("true"),
         "facets" -> Seq("true"),
         "time_histogram" -> Seq("false"))
       
       val parsed = SearchArgs.fromQueryString(validParams)
              
       parsed.query mustBe Some("athens")
       parsed.offset mustBe 10
       parsed.limit mustBe 200
       
       parsed.filters.dateRangeFilter mustBe Some(DateRangeFilter(
         Some(new DateTime(-330, 1, 1, 0, 0)),
         Some(new DateTime(200, 1, 1, 0, 0)),
         DateRangeFilter.INTERSECT
       ))
       
       parsed.filters.placeFilter mustBe Some(PlaceFilter(Seq("http://pleiades.stoa.org/places/579885"), TermFilter.ONLY))
       parsed.filters.languageFilter mustBe Some(TermFilter(Seq("la", "de"), TermFilter.ONLY))
       parsed.filters.categoryFilter mustBe Some(TermFilter(Seq("archaeology"), TermFilter.EXCLUDE))
       parsed.settings.topRelated mustBe true
       parsed.settings.termAggregations mustBe true
       parsed.settings.timeHistogram mustBe false
     }
     
   }
   
   "A spatial filter" should {
     
     "be correctly parseed when center is defined" in {
       val expectedCenter = new Coordinate(16, 48)
       
       val withCenter =
         SearchArgs.fromQueryString(Map("lon" -> Seq("16"), "lat" -> Seq("48")))
       withCenter.filters.spatialFilter mustBe Some(SpatialFilter(None, Some(expectedCenter), None))
         
       val withCenterAndRadius =
         SearchArgs.fromQueryString(Map("lon" -> Seq("16"), "lat" -> Seq("48"), "radius" -> Seq("5")))
       withCenterAndRadius.filters.spatialFilter mustBe Some(SpatialFilter(None, Some(expectedCenter), Some(5.0)))
     }
       
     "be correctly parseed when center is or bbox are defined" in {
       val parsed = SearchArgs.fromQueryString(Map("bbox" -> Seq("15.5,16.5,47,48")))
       parsed.filters.spatialFilter mustBe Some(SpatialFilter(Some(BoundingBox(15.5, 16.5, 47, 48)), None, None))
     }
     
     "fail to parse when lat but not lon is defined" in {
       a [Throwable] should be thrownBy SearchArgs.fromQueryString(Map("lat" -> Seq("16")))
     }
     
     "fail to parse when lat or lon are not Double numbers" in {
       a [Throwable] should be thrownBy SearchArgs.fromQueryString(Map("lat" -> Seq("foo"), "lon" -> Seq("bar")))
     }
     
     "fail to parse when the bounding box is invalid" in {
       a [Throwable] should be thrownBy SearchArgs.fromQueryString(Map("bbox" -> Seq("invalid")))
       a [Throwable] should be thrownBy SearchArgs.fromQueryString(Map("bbox" -> Seq("-10,10,20")))
       a [Throwable] should be thrownBy SearchArgs.fromQueryString(Map("bbox" -> Seq("10,-10,10,-10")))
     }
     
   }
   
   "A date range filter" should {
     
     "accept YYYY format" in {
       val parsed = SearchArgs.fromQueryString(Map("from" -> Seq("2017")))
       parsed.filters.dateRangeFilter.get.from mustBe Some(new DateTime(2017, 1, 1, 0, 0))
     }
     
     "accept YYYY-MM format" in {
       val parsed = SearchArgs.fromQueryString(Map("from" -> Seq("2017-02")))
       parsed.filters.dateRangeFilter.get.from mustBe Some(new DateTime(2017, 2, 1, 0, 0))
     }
     
     "accept YYYY-MM-DD format" in {
       val parsed = SearchArgs.fromQueryString(Map("from" -> Seq("2017-02-28")))
       parsed.filters.dateRangeFilter.get.from mustBe Some(new DateTime(2017, 2, 28, 0, 0))
     }
     
     "fail to parse for invalid formats" in {
       a [Throwable] should be thrownBy SearchArgs.fromQueryString(Map("from" -> Seq("invalid")))
     }
     
   }
   
}