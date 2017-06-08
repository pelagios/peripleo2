package harvesting.crosswalks

import java.io.{ File, FileInputStream, InputStream }
import services.item.{ Description, Depiction, Language, TemporalBounds }
import org.joda.time.{ DateTime, DateTimeZone }
import org.pelagios.Scalagios
import org.pelagios.api.PeriodOfTime
import services.item._

object PelagiosGazetteerCrosswalk {

  private def convertPeriodOfTime(period: PeriodOfTime): TemporalBounds = {
    val startDate = period.start
    val endDate = period.end.getOrElse(startDate)

    TemporalBounds(
      new DateTime(startDate).withZone(DateTimeZone.UTC),
      new DateTime(endDate).withZone(DateTimeZone.UTC))
  }

  def fromRDF(filename: String): InputStream => Seq[ItemRecord] = {

    val sourceGazetteer = filename.substring(0, filename.indexOf('.'))

    def convertPlace(place: org.pelagios.api.gazetteer.Place): ItemRecord =    
      ItemRecord(
        ItemRecord.normalizeURI(place.uri),
        Seq(ItemRecord.normalizeURI(place.uri)),
        DateTime.now().withZone(DateTimeZone.UTC),
        None, // lastChangedAt
        place.label,
        Some(PathHierarchy(sourceGazetteer, sourceGazetteer)),
        None, // isPartOf
        place.category.map(category => Seq(Category(category.toString))).getOrElse(Seq.empty[Category]),
        place.descriptions.map(l => Description(l.chars, l.lang.flatMap(Language.safeParse))),
        None, // homepage
        None, // license
        place.names.flatMap(_.lang).flatMap(Language.safeParse),
        place.depictions.map(image => Depiction(image.uri, None, image.title, None, None, image.license)),
        place.location.map(_.geometry),
        place.location.map(_.pointLocation),
        place.temporalCoverage.map(convertPeriodOfTime(_)),
        place.names.map(l => Name(l.chars, l.lang.flatMap(Language.safeParse))),
        place.closeMatches.map(ItemRecord.normalizeURI),
        place.exactMatches.map(ItemRecord.normalizeURI))
    
    // Return crosswalk function
    { stream: InputStream =>
      Scalagios.readPlaces(stream, filename).map(convertPlace).toSeq }
  }

  def readFile(file: File): Seq[ItemRecord] =
    fromRDF(file.getName)(new FileInputStream(file))

}
