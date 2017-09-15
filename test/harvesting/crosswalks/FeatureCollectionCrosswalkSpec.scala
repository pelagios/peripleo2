package harvesting.crosswalks

import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import java.io.FileInputStream

class FeatureCollectionCrosswalkSpec extends PlaySpec {
  
  private val SAMPLE = "test/resources/harvesting/crosswalks/featurecollection-sample.json"
  
  "The GeoJSON FeatureCollectionCrosswalk" should {
    
    "properly parse the sample collection" in {
      val crosswalk = FeatureCollectionCrosswalk.fromGeoJSON(SAMPLE)
      val in = new FileInputStream(SAMPLE)   
      val records = crosswalk(in)
      
      play.api.Logger.info("got " + records.size + " records")
    }
    
  }
  
}