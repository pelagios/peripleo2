package services.item.reference

import helpers.TestHelpers
import java.util.UUID
import org.scalatestplus.play._
import play.api.libs.json.Json
import play.api.test._
import play.api.test.Helpers._
import services.item.ItemType

class ReferenceSpec extends PlaySpec with TestHelpers {
  
  "Sample object References" should {
    
    "be properly created from JSON" in {
      val objectReferences = loadJSON[Seq[Reference]]("services/item/object/object_references.json")
      objectReferences.size mustBe 2    
      val placeReference = objectReferences(0)     
      placeReference.relation mustBe Some(Relation.FINDSPOT)
      placeReference.referenceTo.uri mustBe "http://pleiades.stoa.org/places/589872"
      placeReference.referenceTo.docId mustBe UUID.fromString("a4a60286-a9af-49ce-969f-177725a579ea")
      placeReference.referenceTo.itemType mustBe ItemType.PLACE
      
      val personReference = objectReferences(1)
      personReference.relation mustBe Some(Relation.ATTESTATION)
      personReference.referenceTo.uri mustBe "http://collection.britishmuseum.org/resource?uri=http://collection.britishmuseum.org/id/person-institution/56988"
      personReference.referenceTo.docId mustBe UUID.fromString("ae12f7eb-e5b1-4fca-9607-fdd212b0e0d4")
      personReference.referenceTo.itemType mustBe ItemType.PERSON
    }
    
  }
  
  "JSON serialization/parsing roundtrip" should {
    
    "yield an equal reference" in {

      val source = Reference(
        "http://www.example.com/objects/0001",
        ReferenceTo(UUID.randomUUID, "http://pleiades.stoa.org/places/118543", ItemType.PLACE, None),
        Some(Relation.ATTESTATION),
        None, // homepage
        None, // quote
        None  // depiction 
      )
      
      val serialized = Json.prettyPrint(Json.toJson(source))
      
      val parsed = Json.fromJson[Reference](Json.parse(serialized))
      parsed.isSuccess mustBe true
      parsed.get mustBe source
    }
    
  }
  
}