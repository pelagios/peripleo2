package services.item

import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._

class PathHierarchySpec extends PlaySpec {
  
  private val SINGLE_HIERARCHY = PathHierarchy(Seq("root", "middle", "leaf"))
  private val SINGLE_HIERARCHY_JSON =
    Json.parse("[ \"root\", \"root\\u0007middle\", \"root\\u0007middle\\u0007leaf\" ]").as[JsArray]
  
  private val HIERARCHY_LIST = Seq(
    PathHierarchy(Seq("rootA", "middleA", "leafA")),
    PathHierarchy(Seq("rootB", "middleB", "leafB")))
  private val HIERARCHY_LIST_JSON =
    Json.parse(
      "[ \"rootA\", \"rootA\\u0007middleA\", \"rootA\\u0007middleA\\u0007leafA\"," +
      "  \"rootB\", \"rootB\\u0007middleB\", \"rootB\\u0007middleB\\u0007leafB\" ]").as[JsArray]

  "Single PathHierarchy" should {
    
    "properly serialize to JSON" in {
      val serialized = Json.toJson(PathHierarchy.fromHierarchy(Some(SINGLE_HIERARCHY)))
      serialized mustBe SINGLE_HIERARCHY_JSON
    }
    
    "be properly restored from JSON" in {
      val levels = SINGLE_HIERARCHY_JSON.value.map(_.as[JsString].value)
      val parsed = PathHierarchy.toHierarchy(Some(levels))
      parsed.isDefined mustBe true
      parsed.get mustBe SINGLE_HIERARCHY
    }
    
  }  
  
  "The PathHierarchy list" should {
    
    "properly serialize to JSON" in {
      val serialized = Json.toJson(PathHierarchy.fromHierarchies(HIERARCHY_LIST))
      serialized mustBe HIERARCHY_LIST_JSON
    }
    
    "be properly restored from JSON" in {
      val levels = HIERARCHY_LIST_JSON.value.map(_.as[JsString].value)
      val parsed = PathHierarchy.toHierarchies(Some(levels))
      parsed.size mustBe 2
      parsed.toSet mustBe HIERARCHY_LIST.toSet // equal, ignoring order
    }
    
  }
  
}