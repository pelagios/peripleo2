package harvesting.crosswalks.periods

import java.io.InputStream
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.item.ItemRecord

object PeriodoCrosswalk {
  
  def parseDefinitionsDump(stream: InputStream): Seq[PeriodoDefinition] = {
    val collections = (Json.parse(stream).as[JsObject] \ "periodCollections").as[JsObject]
    collections.keys.flatMap { collectionId =>
      val collection = (collections \ collectionId \ "definitions").as[JsObject]
      collection.keys.map { definitionId =>
        val json = (collection \ definitionId).as[JsObject]
        Json.fromJson[PeriodoDefinition](json).get                
      }
    }.toSeq
  }

  def fromJSON(filename: String): InputStream => Seq[ItemRecord] = { stream =>
    Seq.empty[ItemRecord]
  }
  
}

case class PeriodoDefinition(id: String, label: String, start: Option[PeriodoDate], stop: Option[PeriodoDate])

case class PeriodoDate(earliestYear: Option[Long], latestYear: Option[Long], year: Option[Long]) {
  
  /** Helper that returns the year or the average of earliest/latest, depending on what's available **/
  lazy val yearOrAverage = (year, earliestYear, latestYear) match {
    // If year is defined, just use this
    case (Some(y), _, _) => year
    
    // If we have both earliest and latest, average
    case (_, Some(e), Some(l)) => Some((e + l) / 2)
    
    // Otherwise, use whatever is available
    case (_, Some(e), _) => earliestYear
    case (_, _, Some(l)) => latestYear
  }
  
}

object PeriodoDate {
  
  implicit val periodoDateReads: Reads[PeriodoDate] = (
    (JsPath \ "earliestYear").readNullable[String].map(_.map(_.toLong)) and
    (JsPath \ "latestYear").readNullable[String].map(_.map(_.toLong)) and
    (JsPath \ "year").readNullable[String].map(_.map(_.toLong))
  )(PeriodoDate.apply _)
  
}

object PeriodoDefinition {
  
  implicit val periodODefinitionReads: Reads[PeriodoDefinition] = (
    (JsPath \ "id").read[String] and
    (JsPath \ "label").read[String] and
    (JsPath \ "start" \ "in").readNullable[PeriodoDate] and
    (JsPath \ "stop" \ "in").readNullable[PeriodoDate]
  )(PeriodoDefinition.apply _)
  
}