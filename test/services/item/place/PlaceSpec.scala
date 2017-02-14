package services.item.place

import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import services.TestHelpers
import services.item.Language

class PlaceSpec extends PlaySpec with TestHelpers {
  
  "Sample Place" should {
        
    "be properly created from JSON" in {
      val place = loadJSON[Place]("services/item/place/place_item.json")
      val records = place.isConflationOf
      records.size mustBe 3
      
      val pleiades = records(0)
      pleiades.uri mustBe "http://pleiades.stoa.org/places/118543"
      pleiades.sourceGazetteer mustBe Gazetteer("Pleiades")
      pleiades.names mustBe Seq(Name("Ad Mauros"))
      pleiades.placeTypes mustBe Seq("fort", "tower")
      
      val dare = records(1)
      dare.uri mustBe "http://dare.ht.lu.se/places/10778"
      dare.sourceGazetteer mustBe Gazetteer("DARE")
      dare.closeMatches must contain ("http://www.wikidata.org/entity/Q2739862")
      
      val trismegistos = records(2)
      trismegistos.uri mustBe "http://www.trismegistos.org/place/35191"
      trismegistos.sourceGazetteer mustBe Gazetteer("Trismegistos")
      trismegistos.names mustBe Seq(
        Name("Ad Mauros"),
        Name("Eferding"),
        Name("Marianianio", Some(Language("LA")))
      )
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