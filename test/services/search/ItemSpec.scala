package services.search

import com.vividsolutions.jts.geom.Coordinate
import org.joda.time.DateTimeField
import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import services.TestHelpers

class ItemSpec extends PlaySpec with TestHelpers {
  
  "Sample Dataset item" should {
    
    "be properly created from JSON" in {
      val datasetItem = loadJSON[Item]("services/search/dataset/dataset_item.json")      
      datasetItem.identifiers mustBe Seq("http://opencontext.org/projects/4B5721E9-2BB3-423F-5D04-1B948FA65FAB")
      datasetItem.itemType mustBe ItemType.DATASET
      datasetItem.lastSyncedAt mustBe Some(toDateTime("2017-02-03T11:18:21Z"))
      datasetItem.lastChangedAt mustBe Some(toDateTime("2014-05-19T12:00:00Z"))
      datasetItem.categories mustBe Seq(Category("Archaeology", Some("http://vocab.getty.edu/aat/300054328")))
      datasetItem.title mustBe "Missouri Site Files"
      datasetItem.isPartOf mustBe Some(PathHierarchy(Seq("http://opencontext.org/projects/")))
      datasetItem.descriptions mustBe Seq(Description("An example dataset with dummy geographical coverage. Some metadata borrowed from OpenContext."))
      datasetItem.homepage mustBe Some("http://opencontext.org/projects/4B5721E9-2BB3-423F-5D04-1B948FA65FAB")
      datasetItem.temporalBounds mustBe Some(TemporalBounds(toDateTime("-1500000-01-01T00:00:00Z"), toDateTime("1500-01-01T00:00:00Z")))
      
      val datasetReferences = loadJSON[Seq[Reference]]("services/search/dataset/dataset_references.json")
      datasetReferences.size mustBe 1
      datasetReferences.head.referenceType mustBe ReferenceType.PLACE
      datasetReferences.head.relation mustBe Some(Relation.COVERAGE)
      datasetReferences.head.representativePoint mustBe Some(new Coordinate(-38.3203125, 37.16031654673677))
      datasetReferences.head.context mustBe None
    }
    
  }
  
  "Sample Object item" should {
    
    "be properly created from JSON" in {
      val objectItem = loadJSON[Item]("services/search/object/object_item.json")       
      objectItem.identifiers mustBe Seq("7beabb84-37f1-4f18-8a74-b02143890bb7", "http://numismatics.org/collection/1991.60.36")
      objectItem.itemType mustBe ItemType.OBJECT
      objectItem.lastSyncedAt mustBe Some(toDateTime("2017-02-03T11:18:21Z"))
      objectItem.lastChangedAt mustBe Some(toDateTime("2016-10-01T12:00:00Z"))
      objectItem.categories mustBe Seq(
        Category("Archaeology", Some("http://vocab.getty.edu/aat/300054328")),
        Category("Numismatics", Some("http://vocab.getty.edu/aat/300054419"))
      )
      objectItem.title mustBe "Silver 4 drachm (tetradrachm), Cnossus, 200 BC - 67 BC. 1991.60.36"
      objectItem.isInDataset mustBe Some(PathHierarchy(Seq(
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
      
      val objectReferences = loadJSON[Seq[Reference]]("services/search/object/object_references.json")
      objectReferences.size mustBe 2
      
      val placeReference = objectReferences(0)      
      placeReference.referenceType mustBe ReferenceType.PLACE
      placeReference.relation mustBe Some(Relation.FINDSPOT)
      placeReference.uri mustBe Some("http://pleiades.stoa.org/places/589872")
      placeReference.representativePoint mustBe Some(new Coordinate(25.163156, 35.297961))
      
      val personReference = objectReferences(1)
      personReference.referenceType mustBe ReferenceType.PERSON
      personReference.relation mustBe Some(Relation.ATTESTATION)
      personReference.uri mustBe Some("http://collection.britishmuseum.org/resource?uri=http://collection.britishmuseum.org/id/person-institution/56988")
    }
    
  }
  
  "Sample Person item" should {
    
    "be properly created from JSON" in {
      val personItem = loadJSON[Item]("services/search/person/person_item.json")
      personItem.identifiers mustBe Seq("http://collection.britishmuseum.org/id/person-institution/56988")
      personItem.itemType mustBe ItemType.PERSON
      personItem.lastSyncedAt mustBe Some(toDateTime("2017-02-03T11:18:21Z"))
      personItem.lastChangedAt mustBe Some(toDateTime("2016-10-01T12:00:00Z"))
      personItem.title mustBe "Apollo"
      personItem.isInDataset mustBe Some(PathHierarchy(Seq("d5e8e113-1552-4f47-9c06-1bb733c8e5be")))
    }
    
  }
  
  "Sample Place item" should {
    
    "be properly created from JSON" in {
       val placeItem = loadJSON[Item]("services/search/place/place_item.json")
       placeItem.identifiers mustBe Seq(
         "http://pleiades.stoa.org/places/118543",
         "http://dare.ht.lu.se/places/10778",
         "http://www.trismegistos.org/place/35191"
       )
       placeItem.itemType mustBe ItemType.PLACE
       placeItem.title mustBe "Ad Mauros/Marinianio, Eferding"
       placeItem.descriptions mustBe Seq(Description("An ancient place, cited: BAtlas 12 H4 Ad Mauros"))
       placeItem.languages mustBe Seq(Language("LA"))
       placeItem.temporalBounds mustBe Some(TemporalBounds(toDateTime("-30-01-01T00:00:00Z"), toDateTime("640-01-01T00:00:00Z")))
       
       val placeReferences = loadJSON[Seq[Reference]]("services/search/place/place_references.json")
       placeReferences.size mustBe 1
       placeReferences.head.referenceType mustBe ReferenceType.PLACE
       placeReferences.head.relation mustBe Some(Relation.COVERAGE)
       placeReferences.head.representativePoint mustBe Some(new Coordinate(14.02358, 48.31058))
    }
    
  }
  
}