package services.item

import com.vividsolutions.jts.geom.Coordinate
import java.util.UUID
import org.joda.time.DateTimeField
import org.scalatestplus.play._
import play.api.libs.json.Json
import play.api.test._
import play.api.test.Helpers._
import services.TestHelpers

class ItemSpec extends PlaySpec with TestHelpers {
  
  "Sample dataset Item" should {
    
    "be properly created from JSON" in {
      val datasetItem = loadJSON[Item]("services/item/dataset/dataset_item.json")      
      datasetItem.identifiers mustBe Seq("http://opencontext.org/projects/4B5721E9-2BB3-423F-5D04-1B948FA65FAB")
      datasetItem.itemType mustBe ItemType.DATASET
      datasetItem.title mustBe "Missouri Site Files"
      datasetItem.lastSyncedAt mustBe Some(toDateTime("2017-02-03T11:18:21Z"))
      datasetItem.lastChangedAt mustBe Some(toDateTime("2014-05-19T12:00:00Z"))
      datasetItem.categories mustBe Seq(Category("Archaeology", Some("http://vocab.getty.edu/aat/300054328")))
      datasetItem.isPartOf mustBe Some(PathHierarchy(Seq("http://opencontext.org/projects/")))
      datasetItem.descriptions mustBe Seq(Description("An example dataset with dummy geographical coverage. Some metadata borrowed from OpenContext."))
      datasetItem.homepage mustBe Some("http://opencontext.org/projects/4B5721E9-2BB3-423F-5D04-1B948FA65FAB")
      datasetItem.representativePoint mustBe Some(new Coordinate(-38.3203125, 37.16031654673677))
      datasetItem.temporalBounds mustBe Some(TemporalBounds(toDateTime("-1500000-01-01T00:00:00Z"), toDateTime("1500-01-01T00:00:00Z")))
    }
    
  }
  
  "Sample object Item" should {
    
    "be properly created from JSON" in {
      val objectItem = loadJSON[Item]("services/item/object/object_item.json")       
     
      objectItem.identifiers mustBe Seq("7beabb84-37f1-4f18-8a74-b02143890bb7", "http://numismatics.org/collection/1991.60.36")
      objectItem.itemType mustBe ItemType.OBJECT
      objectItem.title mustBe "Silver 4 drachm (tetradrachm), Cnossus, 200 BC - 67 BC. 1991.60.36" 
      objectItem.lastSyncedAt mustBe Some(toDateTime("2017-02-03T11:18:21Z"))
      objectItem.lastChangedAt mustBe Some(toDateTime("2016-10-01T12:00:00Z"))                 
      objectItem.categories mustBe Seq(
        Category("Archaeology", Some("http://vocab.getty.edu/aat/300054328")),
        Category("Numismatics", Some("http://vocab.getty.edu/aat/300054419"))
      )
      objectItem.isInDataset mustBe Seq(PathHierarchy(Seq(
        "ecbd9773-b60f-4dc1-bd86-f2cceb6548a1",
        "d6fdd35c-08b6-495b-ad4e-3f9256d30665"
      )))          
      objectItem.temporalBounds mustBe Some(TemporalBounds(toDateTime("-200-01-01T00:00:00Z"), toDateTime("67-01-01T00:00:00Z")))
      objectItem.depictions mustBe Seq(
        Depiction("http://numismatics.org/collectionimages/19501999/1991/1991.60.36.obv.width350.jpg",
          Some("http://numismatics.org/collectionimages/19501999/1991/1991.60.36.obv.width175.jpg"),
          Some("Obverse: Head of Apollo laureate"),
          None, None, None),
        Depiction("http://numismatics.org/collectionimages/19501999/1991/1991.60.36.rev.width350.jpg",
          Some("http://numismatics.org/collectionimages/19501999/1991/1991.60.36.obv.width175.jpg"),
          Some("Reverse: Circular Labyrinth"),
          None, None, None)
      )
      
      val objectReferences = loadJSON[Seq[Reference]]("services/item/object/object_references.json")
      objectReferences.size mustBe 2
      
      val placeReference = objectReferences(0)     
      placeReference.referenceType mustBe ReferenceType.PLACE
      placeReference.relation mustBe Some(Relation.FINDSPOT)
      placeReference.uri mustBe "http://pleiades.stoa.org/places/589872"
      placeReference.rootUri mustBe "http://pleiades.stoa.org/places/589872"
      
      val personReference = objectReferences(1)
      personReference.referenceType mustBe ReferenceType.PERSON
      personReference.relation mustBe Some(Relation.ATTESTATION)
      personReference.uri mustBe "http://collection.britishmuseum.org/resource?uri=http://collection.britishmuseum.org/id/person-institution/56988"
      personReference.rootUri mustBe "http://collection.britishmuseum.org/resource?uri=http://collection.britishmuseum.org/id/person-institution/56988"
    }
    
  }
  
  "Sample period Item" should {
    
    "be properly created from JSON" in {
      val periodItem = loadJSON[Item]("services/item/period/period_item.json")
      periodItem.identifiers mustBe Seq("http://n2t.net/ark:/99152/p0z5nvh24r6")
      periodItem.itemType mustBe ItemType.PERIOD
      periodItem.title mustBe "Aegean Bronze Age"
      periodItem.lastSyncedAt mustBe Some(toDateTime("2017-02-14T07:49:00Z"))
      periodItem.descriptions mustBe Seq(Description("In collection: Stokstad, Marilyn, 1929-. Art history. 2005."))
      periodItem.temporalBounds mustBe Some(TemporalBounds.fromYears(-2999, -1999))
    }
    
  }
  
  "Sample person Item" should {
    
    "be properly created from JSON" in {
      val personItem = loadJSON[Item]("services/item/person/person_item.json")
      personItem.identifiers mustBe Seq("http://collection.britishmuseum.org/id/person-institution/56988")
      personItem.itemType mustBe ItemType.PERSON
      personItem.title mustBe "Apollo"
      personItem.lastSyncedAt mustBe Some(toDateTime("2017-02-03T11:18:21Z"))
      personItem.lastChangedAt mustBe Some(toDateTime("2016-10-01T12:00:00Z"))
      personItem.isInDataset mustBe Seq(PathHierarchy(Seq("d5e8e113-1552-4f47-9c06-1bb733c8e5be")))
    }
    
  }
  
  "Sample place Item" should {
    
    "be properly created from JSON" in {
       val placeItem = loadJSON[Item]("services/item/place/place_item.json")
       placeItem.identifiers mustBe Seq(
         "http://pleiades.stoa.org/places/118543",
         "http://dare.ht.lu.se/places/10778",
         "http://www.trismegistos.org/place/35191"
       )
       placeItem.itemType mustBe ItemType.PLACE
       placeItem.title mustBe "Ad Mauros/Marinianio, Eferding"
       placeItem.languages mustBe Seq(Language("LA"))
       placeItem.representativePoint mustBe Some(new Coordinate(14.02358, 48.31058))
       placeItem.temporalBounds mustBe Some(TemporalBounds(toDateTime("-30-01-01T00:00:00Z"), toDateTime("640-01-01T00:00:00Z")))
       
       val placeReferences = loadJSON[Seq[Reference]]("services/item/place/place_references.json")
       placeReferences.size mustBe 1
       placeReferences.head.referenceType mustBe ReferenceType.PLACE
       placeReferences.head.relation mustBe None
    }
    
  }
  
  "JSON serialization/parsing roundtrip" should {
    
    "yield an equal item" in {
      val point = createPoint(14.02358, 48.31058)
      
      val source = Item(
        Seq("7beabb84-37f1-4f18-8a74-b02143890bb7", "http://numismatics.org/collection/1991.60.36"),
        ItemType.OBJECT,
        "A dummy object",
        Some(CURRENT_TIME),
        Some(CURRENT_TIME),
        Seq(Category("Archaeology", Some("http://vocab.getty.edu/aat/300054328"))),
        Seq(PathHierarchy(Seq(UUID.randomUUID.toString))),
        None, // is_part_of
        Seq(Description("Just a dummy object for the roundtrip test")),
        Some("http://www.example.com"),
        None,
        Seq.empty[Language],
        Some(point),
        Some(point.getCoordinate),
        Some(TemporalBounds.fromYears(-500, -250)),
        Seq.empty[String], // periods
        Seq(
          Depiction("http://www.example.com/images/001.jpg", None, Some("Fig. 1"), None, None, None),
          Depiction("http://www.example.com/images/002.jpg", None, Some("Fig. 2"), None, None, None)
        )
      )
      
      val serialized = Json.prettyPrint(Json.toJson(source))
      
      val parsed = Json.fromJson[Item](Json.parse(serialized))
      parsed.isSuccess mustBe true
      parsed.get mustBe source
    }
    
    "yield an equal reference" in {

      val source = Reference(
        ReferenceType.PLACE,
        Some(Relation.ATTESTATION),
        "http://pleiades.stoa.org/places/118543",
        "http://pleiades.stoa.org/places/118543",
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