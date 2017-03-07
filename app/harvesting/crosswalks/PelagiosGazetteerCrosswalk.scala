package harvesting.crosswalks

import java.io.InputStream
import services.item.{ Description, Language, TemporalBounds }
import services.item.place._
import org.joda.time.{ DateTime, DateTimeZone }
import org.pelagios.Scalagios
import org.pelagios.api.PeriodOfTime
import java.io.File
import java.io.FileInputStream

object PelagiosGazetteerCrosswalk {
  
  private def convertPeriodOfTime(period: PeriodOfTime): TemporalBounds = {
    val startDate = period.start
    val endDate = period.end.getOrElse(startDate)
    
    TemporalBounds(
      new DateTime(startDate).withZone(DateTimeZone.UTC), 
      new DateTime(endDate).withZone(DateTimeZone.UTC))          
  }
  
  def fromRDF(filename: String): InputStream => Seq[GazetteerRecord] = {
    
    val sourceGazetteer = Gazetteer(filename.substring(0, filename.indexOf('.')))
  
    def convertPlace(place: org.pelagios.api.gazetteer.Place): GazetteerRecord =
      GazetteerRecord(
        GazetteerRecord.normalizeURI(place.uri),
        sourceGazetteer,
        DateTime.now().withZone(DateTimeZone.UTC),
        None, // last_changed_at
        place.label,
        place.descriptions.map(l => Description(l.chars, l.lang.map(Language(_)))),
        place.names.map(l => Name(l.chars, l.lang.map(Language(_)))),
        place.location.map(_.geometry),
        place.location.map(_.pointLocation),
        place.temporalCoverage.map(convertPeriodOfTime(_)),
        place.category.map(category => Seq(category.toString)).getOrElse(Seq.empty[String]),
        place.closeMatches.map(GazetteerRecord.normalizeURI(_)),
        place.exactMatches.map(GazetteerRecord.normalizeURI(_)))
    
    // Return crosswalk function
    { stream: InputStream =>
      Scalagios.readPlaces(stream, filename).map(convertPlace).toSeq }
  }
  
  def readFile(file: File): Seq[GazetteerRecord] =
    fromRDF(file.getName)(new FileInputStream(file))
  
}