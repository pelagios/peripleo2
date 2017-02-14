package services.item

import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json.Json

class PathHierarchySpec extends PlaySpec {
  
  private val AS_HIERARCHY = PathHierarchy(Seq("foo", "bar", "baz"))
  
  private val AS_JSON = Json.parse("[ \"foo\", \"foo\\u0007bar\", \"foo\\u0007bar\\u0007baz\" ]")
  
  "PathHierarchy" should {
    
    "properly serialize to JSON" in {
      val serialized = Json.toJson(AS_HIERARCHY)
      serialized mustBe AS_JSON
    }
    
    "be properly restored from JSON" in {
      val parsed = Json.fromJson[PathHierarchy](AS_JSON)
      parsed.isSuccess mustBe true
      parsed.get mustBe AS_HIERARCHY
    }
    
  }  
  
}