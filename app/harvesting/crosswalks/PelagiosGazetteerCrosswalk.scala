package harvesting.crosswalks

import java.io.{ File, FileInputStream, InputStream }
import services.item.{ Description, Depiction, Language, TemporalBounds }
import org.joda.time.{ DateTime, DateTimeZone }
import org.pelagios.Scalagios
import org.pelagios.api.TimeInterval
import services.item._

object PelagiosGazetteerCrosswalk {

  private def convertTimeInterval(period: TimeInterval): TemporalBounds = {
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
        place.depictions.map(image => Depiction(image.uri, DepictionType.IMAGE, None, image.title, None, None, image.license)),
        place.location.map(_.geometry),
        place.location.map(_.pointLocation),
        place.timeInterval.map(convertTimeInterval(_)),
        place.names.map(l => Name(l.chars, l.lang.flatMap(Language.safeParse))),
        place.closeMatches.map(Link(_, LinkType.CLOSE_MATCH)) ++ place.exactMatches.map(Link(_, LinkType.EXACT_MATCH)), 
        None, None)
    
    // Return crosswalk function
    { stream: InputStream =>
      Scalagios.readPlaces(stream, filename).map(convertPlace).toSeq }
  }

  def readFile(file: File): Seq[ItemRecord] =
    fromRDF(file.getName)(new FileInputStream(file))

}
