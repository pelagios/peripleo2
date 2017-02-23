package services.search

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.source.JsonDocumentSource
import org.apache.commons.io.FileUtils
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json.{ Json, JsArray, JsObject }
import play.api.libs.concurrent.Execution.Implicits._
import services.TestHelpers
import scala.io.Source
import scala.language.reflectiveCalls
import org.scalatest.ConfigMap


class QuerySpec extends PlaySpec with TestHelpers with BeforeAndAfterAll {
  
  /*
  private val (indexDir, es) = {
    val (indexDir, es) = createEmbeddedES("peripleo")
    
    val mapping = loadTextfile("services/item/search/result_mapping.json")
    val put = es.admin.indices().preparePutMapping("peripleo")
    put.setType("item")
    put.setSource(mapping)
    put.execute().actionGet()
    
    val json = Json.parse(loadTextfile("services/item/search/results.json")).as[JsArray].value
    json.foreach(js =>
    es.execute { index into "peripleo" / "item" doc JsonDocumentSource(Json.stringify(js)) }.await)
      
    Thread.sleep(1000)
    
    (indexDir, es)
  }

  override def afterAll() = FileUtils.deleteDirectory(indexDir)
  
  private def runQuery(q: String) =
    es.execute {
      search in "peripleo" / "item" query {
        bool {
          should(
            termQuery("title", q),
            nestedQuery("references") query { termQuery("references.context", q) } inner("matched_contexts")
          )
        }
      } aggregations (
        aggregation nested("by_place") path "references" aggs (
          aggregation
            terms "place"
            field "references.uri"
            size 20
            

        )
      )
    }
  
  "In total, the test index" should {
    
    "contain 3 documents" in {
      val count = es.execute { search in "peripleo" size 0 } map { _.totalHits }
      await(count) mustBe 3
    }
    
  }
  
  "A search for 'coin'" should {
    
    "return the correct two results" in {
      val result = runQuery("coin").await
      play.api.Logger.info(result.toString)
      
      result.totalHits mustBe 2
     
      val firstHit =
        Json.parse(result.hits(0).sourceAsString).as[JsObject]
      
      
      val secondHit =
        Json.parse(result.hits(0).sourceAsString).as[JsObject]
      
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
    
  }*/
  
}
  