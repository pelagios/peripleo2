package services.item.place

import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json.Json
import services.TestHelpers
import services.item._

class PlaceSpec extends PlaySpec with TestHelpers {
  
  private val EXAMPLE_PLACE = Place("http://pleiades.stoa.org/places/118543", Seq(
    GazetteerRecord(
      "http://pleiades.stoa.org/places/118543",
      Gazetteer("Pleiades"),
      toDateTime("2016-04-03T11:23:00Z"),
      None, // last_changed_at
      "Ad Mauros",
      Seq.empty[Description],
      Seq(Name("Ad Mauros")),
      Some(createPoint(14.02358, 48.31058)),
      Some(createPoint(14.02358, 48.31058).getCoordinate),
      Some(TemporalBounds.fromYears(0, 640)),
      Seq("fort", "tower"),
      Seq.empty[String],
      Seq.empty[String]
    ),
    
    GazetteerRecord(
      "http://dare.ht.lu.se/places/10778",
      Gazetteer("DARE"),
      toDateTime("2016-04-03T11:23:00Z"),
      None, // last_changed_at
      "Ad Mauros/Marinianio, Eferding",
      Seq.empty[Description],
      Seq(Name("Ad Mauros/Marinianio, Eferding", Some(Language("LA")))),
      Some(createPoint(14.02358, 48.31058)),
      Some(createPoint(14.02358, 48.31058).getCoordinate),
      Some(TemporalBounds.fromYears(-30, 0)),
      Seq.empty[String],
      Seq("http://sws.geonames.org/2780394", "http://www.wikidata.org/entity/Q2739862", "http://de.wikipedia.org/wiki/Kastell_Eferding"),
      Seq.empty[String]  
    )
  ))
  
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
      val serialized = Json.prettyPrint(Json.toJson(EXAMPLE_PLACE))
      val parsed = Json.fromJson[Place](Json.parse(serialized))
      parsed.isSuccess mustBe true
      parsed.get mustBe EXAMPLE_PLACE
    }
    
  }
  
  "A serialized Place" should {
    
    "also satisfy the schema requirements for Items" in {
      val serialized = Json.prettyPrint(Json.toJson(EXAMPLE_PLACE))
      val parsed = Json.fromJson[Item](Json.parse(serialized))
      parsed.isSuccess mustBe true
      
      val placeItem = parsed.get
      placeItem.identifiers mustBe Seq("http://pleiades.stoa.org/places/118543", "http://dare.ht.lu.se/places/10778")
      placeItem.itemType mustBe ItemType.PLACE
      placeItem.title mustBe "Ad Mauros"
      placeItem.languages mustBe Seq(Language("LA"))
      placeItem.temporalBounds mustBe Some(TemporalBounds.fromYears(-30, 640))
    }
    
  }
  
}