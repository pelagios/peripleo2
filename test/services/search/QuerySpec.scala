package services.search

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.source.JsonDocumentSource
import org.apache.commons.io.FileUtils
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json.{ Json, JsArray }
import play.api.libs.concurrent.Execution.Implicits._
import services.TestHelpers
import scala.io.Source
import org.scalatest.ConfigMap


class QuerySpec extends PlaySpec with TestHelpers with BeforeAndAfterAll {
  
  private val (indexDir, es) = {
    val (indexDir, es) = createEmbeddedES("peripleo")
    
    val mapping = Source.fromFile("conf/es-mappings/01_item.json").getLines().mkString("\n")  
    val put = es.admin.indices().preparePutMapping("peripleo")
    put.setType("item")
    put.setSource(mapping)
    put.execute().actionGet()
    
    val json = Json.parse(loadTextfile("services/item/search/results.json")).as[JsArray].value
    json.foreach(js =>
    es.execute { index into "peripleo" / "item" doc JsonDocumentSource(Json.stringify(js)) }.await)
      
    Thread.sleep(2000)
    
    (indexDir, es)
  }

  override def afterAll() = FileUtils.deleteDirectory(indexDir)
  
  "In total, the test index" should {
    
    "contain 3 documents" in {
      val count = es.execute { search in "peripleo" size 0 } map { _.totalHits }
      await(count) mustBe 3
    }
    
  }
  
  "A search for 'coin'" should {
    
    "return the correct two results" in {
      
    }
    
    "return two hits on Athens" in {
      
    }
    
    "return one hit on Vindobona" in {
      
    }
    
  }
  
  "A search for 'Athens'" should {
   
    "return the correct three results" in {
      
    }
    
    "return three hits on Athens" in {
      
    }
    
  }
  
}
  