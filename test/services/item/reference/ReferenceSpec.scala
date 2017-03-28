package services.item.reference

import java.util.UUID
import org.scalatestplus.play._
import play.api.libs.json.Json
import play.api.test._
import play.api.test.Helpers._
import services.TestHelpers

class ReferenceSpec extends PlaySpec with TestHelpers {
  
  "Sample object References" should {
    
    "be properly created from JSON" in {
      val objectReferences = loadJSON[Seq[Reference]]("services/item/object/object_references.json")
      objectReferences.size mustBe 2    
      val placeReference = objectReferences(0)     
      placeReference.referenceType mustBe ReferenceType.PLACE
      placeReference.relation mustBe Some(Relation.FINDSPOT)
      placeReference.referenceTo.uri mustBe "http://pleiades.stoa.org/places/589872"
      placeReference.referenceTo.docId mustBe UUID.fromString("a4a60286-a9af-49ce-969f-177725a579ea")
      
      val personReference = objectReferences(1)
      personReference.referenceType mustBe ReferenceType.PERSON
      personReference.relation mustBe Some(Relation.ATTESTATION)
      personReference.referenceTo.uri mustBe "http://collection.britishmuseum.org/resource?uri=http://collection.britishmuseum.org/id/person-institution/56988"
      personReference.referenceTo.docId mustBe UUID.fromString("ae12f7eb-e5b1-4fca-9607-fdd212b0e0d4")   
    }
    
  }
  
  "Sample place Reference" should {
    
    "be properly created from JSON" in {
       val placeReferences = loadJSON[Seq[Reference]]("services/item/place/place_references.json")
       placeReferences.size mustBe 1
       placeReferences.head.referenceType mustBe ReferenceType.PLACE
       placeReferences.head.relation mustBe None
    }
    
  }
  
  "JSON serialization/parsing roundtrip" should {
    
    "yield an equal reference" in {

      val source = Reference(
        ReferenceType.PLACE,
        ReferenceTo("http://pleiades.stoa.org/places/118543", UUID.randomUUID),
        Some(Relation.ATTESTATION),
        None, // homepage
        None, // context
        None  // depiction 
      )
      
      val serialized = Json.prettyPrint(Json.toJson(source))
      
      val parsed = Json.fromJson[Reference](Json.parse(serialized))
      parsed.isSuccess mustBe true
      parsed.get mustBe source
    }
    
  }
  
}