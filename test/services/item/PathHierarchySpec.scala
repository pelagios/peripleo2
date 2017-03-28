package services.item

import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._

class PathHierarchySpec extends PlaySpec {
  
  private val SINGLE_HIERARCHY = PathHierarchy(Seq(
    PathSegment("id01", "Root"),
    PathSegment("id02", "Middle"),
    PathSegment("id03", "Leaf")
  ))
  private val SINGLE_HIERARCHY_JSON =
    Json.parse("[ \"id01\\u0007Root\", " + 
                 "\"id01\\u0007Root\\u0007\\u0007id02\\u0007Middle\", " +
                 "\"id01\\u0007Root\\u0007\\u0007id02\\u0007Middle\\u0007\\u0007id03\\u0007Leaf\" ]").as[JsArray]
  
  private val HIERARCHY_LIST = Seq(
    PathHierarchy(Seq(
      PathSegment("idA01", "RootA"),
      PathSegment("idA02", "MiddleA"),
      PathSegment("idA03", "LeafA"))),
    PathHierarchy(Seq(
      PathSegment("idB01", "RootB"),
      PathSegment("idB02", "MiddleB"),
      PathSegment("idB03", "LeafB"))))
  private val HIERARCHY_LIST_JSON =
    Json.parse(
      "[ \"idA01\\u0007RootA\", " +
        "\"idA01\\u0007RootA\\u0007\\u0007idA02\\u0007MiddleA\", " + 
        "\"idA01\\u0007RootA\\u0007\\u0007idA02\\u0007MiddleA\\u0007\\u0007idA03\\u0007LeafA\", " +
        "\"idB01\\u0007RootB\", " + 
        "\"idB01\\u0007RootB\\u0007\\u0007idB02\\u0007MiddleB\", " +
        "\"idB01\\u0007RootB\\u0007\\u0007idB02\\u0007MiddleB\\u0007\\u0007idB03\\u0007LeafB\" ]").as[JsArray]

  "Single PathHierarchy" should {
    
    "properly serialize to JSON" in {
      val serialized = Json.toJson(PathHierarchy.fromHierarchy(SINGLE_HIERARCHY))
      serialized mustBe SINGLE_HIERARCHY_JSON
    }
    
    "be properly restored from JSON" in {
      val levels = SINGLE_HIERARCHY_JSON.value.map(_.as[JsString].value)
      val parsed = PathHierarchy.toHierarchy(levels)
      parsed mustBe SINGLE_HIERARCHY
    }
    
  }  
  
  "The PathHierarchy list" should {
    
    "properly serialize to JSON" in {
      val serialized = Json.toJson(PathHierarchy.fromHierarchies(HIERARCHY_LIST))
      serialized mustBe HIERARCHY_LIST_JSON
    }
    
    "be properly restored from JSON" in {
      val levels = HIERARCHY_LIST_JSON.value.map(_.as[JsString].value)
      val parsed = PathHierarchy.toHierarchies(levels)
      parsed.size mustBe 2
      parsed.toSet mustBe HIERARCHY_LIST.toSet // equal, ignoring order
    }
    
  }
  
}