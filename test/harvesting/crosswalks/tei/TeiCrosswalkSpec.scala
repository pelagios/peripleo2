package harvesting.crosswalks.tei

import java.io.FileInputStream
import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import services.item.PathHierarchy

class TeiCrosswalkSpec extends PlaySpec {
  
  private val TEI = "test/resources/harvesting/crosswalks/tei/bordeaux-itinerary-en.tei.xml"
  
  private val DATASET = PathHierarchy("http://recogito.pelagios.org/pelagios3", "Pelagios 3 Samples")
  
  private val ID = DATASET.ids.mkString("/") + "bordeaux-itinerary-en"
  
  "The TEI crosswalk" should {
    
    "produce a single ItemRecord from the TEI" in {
      val (record, _) = TeiCrosswalk.parseTEIXML(ID, DATASET, new FileInputStream(TEI))
      record.title mustBe "Anonymous: Bordeaux Itinerary"
      record.homepage mustBe Some("http://www.christusrex.org/www1/ofm/pilgr/bord/10Bord01Lat.html")
    }
    
    "generate the correct references" in {
      val (_, references) = TeiCrosswalk.parseTEIXML(ID, DATASET, new FileInputStream(TEI))
      references.size mustBe 392
      references.foreach { ref =>        
        ref.parentUri mustBe "http://recogito.pelagios.org/pelagios3bordeaux-itinerary-en"
        ref.uri.startsWith("http://pleiades.stoa.org/places/") mustBe true
        ref.quote.isDefined mustBe true
      }      
    }
    
  }
  
}