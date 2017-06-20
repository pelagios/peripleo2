package harvesting.crosswalks.periods

import java.io.InputStream
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.item.ItemRecord

object PeriodOCrosswalk {
  
  def parseDefinitionsDump(stream: InputStream): Seq[PeriodODefinition] = {
    val collections = (Json.parse(stream).as[JsObject] \ "periodCollections").as[JsObject]
    collections.keys.flatMap { collectionId =>
      val collection = (collections \ collectionId \ "definitions").as[JsObject]
      collection.keys.map { definitionId =>
        val json = (collection \ definitionId).as[JsObject]
        // play.api.Logger.info(json.toString)
        Json.fromJson[PeriodODefinition](json).get                
      }
    }.toSeq
  }

  def fromJSON(filename: String): InputStream => Seq[ItemRecord] = { stream =>
    Seq.empty[ItemRecord]
  }
  
}

case class PeriodODefinition(id: String, label: String, startYear: String, stopYear: String)

object PeriodODefinition {
  
  implicit val periodODefinitionReads: Reads[PeriodODefinition] = (
    (JsPath \ "id").read[String] and
    (JsPath \ "label").read[String] and
    (JsPath \ "start" \ "in" \ "year").read[String] and
    (JsPath \ "stop" \ "in" \ "year").read[String]
  )(PeriodODefinition.apply _)
  
}