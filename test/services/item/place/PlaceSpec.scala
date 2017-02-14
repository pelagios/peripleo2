package services.item.place

import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import services.TestHelpers

class PlaceSpec extends PlaySpec with TestHelpers {
  
  "Sample Place" should {
    
    // Places are also items - most of the parsing is tested in the ItemSpec already
        
    "be properly created from JSON" in {
      val place = loadJSON[Place]("services/search/place/place_item.json")
       
      // TODO
      
    }
    
  }
  
  "JSON serialization/parsing roundtrip" should {
    
    "yield an equal Place" in {
      
      // TODO
      
    }
    
  }
  
  "A serialized Place" should {
    
    "also satisfy the schema requirements for Items" in {
      
      // TODO
      
    }
    
  }
  
}