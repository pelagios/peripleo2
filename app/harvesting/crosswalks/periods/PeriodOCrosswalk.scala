package harvesting.crosswalks.periods

import java.io.InputStream
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.util.Try
import services.item._

object PeriodoCrosswalk {
  
  private[periods] def parseDefinitionsDump(stream: InputStream): Seq[(String, PeriodoDefinition)] = {
    val collections = (Json.parse(stream).as[JsObject] \ "periodCollections").as[JsObject]
    collections.keys.flatMap { collectionId =>
      val collection = (collections \ collectionId \ "definitions").as[JsObject]
      collection.keys.map { definitionId =>
        val json = (collection \ definitionId).as[JsObject]
        
        // Unfortunately, PeriodO doesn't include the URI, only the definition ID.
        // However, the URI is built from the definition ID as well as the collection ID.
        val maybeDefinition = Try(Json.fromJson[PeriodoDefinition](json).get).toOption
        maybeDefinition.map(definition => (collectionId, definition))                
      }.flatten
    }.toSeq
  }
  
  private def toItemRecord(collectionId: String, p: PeriodoDefinition, dataset: PathHierarchy) = {
    val uri = "http://n2t.net/ark:/" + collectionId + "/" + p.id
      
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
      None,
      Seq.empty[String], Seq.empty[String])
  }

  def fromJSON(filename: String, dataset: PathHierarchy): InputStream => Seq[ItemRecord] = { stream =>
    parseDefinitionsDump(stream).map(t => toItemRecord(t._1, t._2, dataset))
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