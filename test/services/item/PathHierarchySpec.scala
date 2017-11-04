package services.item

import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._

class PathHierarchySpec extends PlaySpec {

  private val HIERARCHY = PathHierarchy(Seq(
    ("id01" -> "Root"),
    ("id02" -> "Middle"),
    ("id03" -> "Leaf")
  ))

  private val HIERARCHY_JSON =
    Json.parse("{" +
                  "\"root\": \"id01\\u0007Root\"," + 
                  "\"paths\": [ \"id01\\u0007Root\", " +
                                "\"id01\\u0007Root\\u0007\\u0007id02\\u0007Middle\", " +
                                "\"id01\\u0007Root\\u0007\\u0007id02\\u0007Middle\\u0007\\u0007id03\\u0007Leaf\" ]," +
                  "\"ids\": [\"id01\", \"id02\", \"id03\" ]" +
               "}").as[JsObject]

  "The PathHierarchy" should {

    "properly serialize to JSON" in {
      val serialized = Json.toJson(HIERARCHY)
      serialized mustBe HIERARCHY_JSON
    }

    "be properly restored from JSON" in {
      val parsed = Json.fromJson[PathHierarchy](HIERARCHY_JSON)
      parsed.isSuccess mustBe true
      parsed.get mustBe HIERARCHY
    }

  }

}
