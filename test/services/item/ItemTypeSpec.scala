package services.item

import org.scalatestplus.play._
import play.api.libs.json._
import play.api.test._
import play.api.test.Helpers._

class ItemTypeSpec extends PlaySpec {
  
  // Just use the most deeply-nested type as a test
  val GAZETTEER_TYPE = ItemType.DATASET.AUTHORITY.GAZETTEER
  val GAZETTEER_JSON = Json.parse("[ \"DATASET\", \"AUTHORITY\", \"AUTHORITY_GAZETTEER\" ]").as[JsArray]
  
  "ItemType" should {
    
    "properly serialize to JSON" in {
      val serialized = Json.toJson(GAZETTEER_TYPE)
      serialized mustBe GAZETTEER_JSON
    }
    
    "be properly restored from JSON" in {
      val parsed = Json.fromJson[ItemType](GAZETTEER_JSON)
      parsed.isSuccess mustBe true
      parsed.get mustBe GAZETTEER_TYPE
    }
    
  }
  
}