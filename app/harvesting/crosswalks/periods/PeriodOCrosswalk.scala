package harvesting.crosswalks.periods

import java.io.InputStream
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.util.Try
import services.item._

object PeriodoCrosswalk {
  
  private[periods] def parseDefinitionsDump(stream: InputStream): (String, Seq[PeriodoDefinition]) = {
    val json = Json.parse(stream).as[JsObject]    
    val base = (json \ "@context" \ "@base").as[JsString].value
    val collections = (json \ "periodCollections").as[JsObject]
    
    val definitions = collections.keys.flatMap { collectionId =>
      val collection = (collections \ collectionId \ "definitions").as[JsObject]
      collection.keys.map { definitionId =>
        val json = (collection \ definitionId).as[JsObject]
        
        // Unfortunately, PeriodO doesn't include the URI, only the definition ID.
        // However, the URI is built from the definition ID as well as the collection ID.
        Try(Json.fromJson[PeriodoDefinition](json).get).toOption        
      }.flatten
    }.toSeq
    
    (base, definitions)
  }
  
  private def toItemRecord(uri: String, p: PeriodoDefinition, dataset: PathHierarchy) = {
    ItemRecord(
      uri,
      Seq(uri),
      DateTime.now,
      None, // lastChangedAt
      p.label,
      Some(dataset),
      None, // isPartOf
      Seq.empty[Category],
      Seq.empty[Description],
      None, None, // homepage, license
      Seq.empty[Language],
      Seq.empty[Depiction],
      None, None, // geometry, representativePoint
      p.temporalBounds,
      Seq.empty[Name],
      Seq.empty[Link],
      None, None)
  }

  def fromJSON(filename: String, dataset: PathHierarchy): InputStream => Seq[ItemRecord] = { stream =>
    val (base, definitions) = parseDefinitionsDump(stream)
    definitions.map(d => toItemRecord(base + d.id, d, dataset))
  }
  
}

case class PeriodoDefinition(id: String, label: String, start: Option[PeriodoDate], stop: Option[PeriodoDate]) {
  
  val temporalBounds = (start, stop) match {
    case (Some(a), Some(b)) => Some(TemporalBounds.fromYears(a.min, b.max))
    case (Some(a), _)       => Some(TemporalBounds.fromYears(a.min, a.max))
    case (_, Some(b))       => Some(TemporalBounds.fromYears(b.min, b.max))
    case _                  => None
  }
  
}

case class PeriodoDate(earliestYear: Option[Int], latestYear: Option[Int], year: Option[Int]) {
  
  /** Helper that returns the year or the average of earliest/latest, depending on what's available **/
  val yearOrAverage = (year, earliestYear, latestYear) match {
    // If year is defined, just use this
    case (Some(y), _, _) => year
    
    // If we have both earliest and latest, average
    case (_, Some(e), Some(l)) => Some((e + l) / 2)
    
    // Otherwise, use whatever is available
    case (_, Some(e), _) => earliestYear
    case (_, _, Some(l)) => latestYear
    
    // Just to avoid compiler warning
    case _ => throw new RuntimeException
  }
  
  val min = Seq(earliestYear, latestYear, year).flatten.min
    
  val max = Seq(earliestYear, latestYear, year).flatten.max
  
}

object PeriodoDate {
  
  implicit val periodoDateReads: Reads[PeriodoDate] = (
    (JsPath \ "earliestYear").readNullable[String].map(_.map(_.toInt)) and
    (JsPath \ "latestYear").readNullable[String].map(_.map(_.toInt)) and
    (JsPath \ "year").readNullable[String].map(_.map(_.toInt))
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