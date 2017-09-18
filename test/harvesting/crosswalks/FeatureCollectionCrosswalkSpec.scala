package harvesting.crosswalks

import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import java.io.FileInputStream
import services.item.PathHierarchy

class FeatureCollectionCrosswalkSpec extends PlaySpec {
  
  private val SAMPLE = "test/resources/harvesting/crosswalks/featurecollection-sample.json"
  
  "The GeoJSON FeatureCollectionCrosswalk" should {
    
    "properly parse the sample collection" in {
      val dataset = PathHierarchy("http://www.example.com/gazetteer", "Example Gazetteer")
        
      val crosswalk = FeatureCollectionCrosswalk.fromGeoJSON(dataset)
      val in = new FileInputStream(SAMPLE)   
      val records = crosswalk(in)
      
      records.size mustBe 9
      
      val sample = records.find(_.uri == "http://www.cyprusgazetteer.org/146853/551")
      sample.isDefined mustBe true
      sample.get.exactMatches.size mustBe 0
      sample.get.closeMatches.size mustBe 1
      sample.get.closeMatches.head mustBe "http://sws.geonames.org/146853"
      
      play.api.Logger.info("got " + records.size + " records")
    }
    
  }
  
}