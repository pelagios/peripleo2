package harvesting.crosswalks

import java.io.InputStream
import java.util.UUID
import org.joda.time.{ DateTime, DateTimeZone }
import org.pelagios.Scalagios
import org.pelagios.api.PeriodOfTime
import org.pelagios.api.annotation.AnnotatedThing
import services.item._
import services.item.reference.UnboundReference
import scala.util.Try

object PelagiosAnnotationCrosswalk {

  // Bit annoying that this is duplication with the Place crosswalk - but would
  // rather have those few lines of duplication than pollute the code with a
  // Pelagios-RDF-specific trait
  private def convertPeriodOfTime(period: PeriodOfTime): TemporalBounds = {
    val startDate = period.start
    val endDate = period.end.getOrElse(startDate)

    TemporalBounds(
      new DateTime(startDate).withZone(DateTimeZone.UTC),
      new DateTime(endDate).withZone(DateTimeZone.UTC))
  }

  /** Returns a flat list of all things below this thing in the hierarchy **/
  private def flattenThingHierarchy(thing: AnnotatedThing): Seq[AnnotatedThing] =
    if (thing.parts.isEmpty) thing.parts
    else thing.parts ++ thing.parts.flatMap(flattenThingHierarchy)

  def fromRDF(filename: String, inDataset: PathHierarchy): InputStream => Seq[(ItemRecord, Seq[UnboundReference])] = {

    def convertAnnotatedThing(thing: AnnotatedThing): Seq[(ItemRecord, Seq[UnboundReference])] = {
      val flattenedHierarchy = thing +: flattenThingHierarchy(thing)
      flattenedHierarchy.map { thing =>

        val references = thing.annotations.flatMap { _.places.headOption.map { placeUri =>
          val uri = ItemRecord.normalizeURI(placeUri)

          UnboundReference(
            thing.uri,
            uri,
            None, // relation
            None, // homepage
            None, // quote
            None  // depiction
          )
        }}

        val record = ItemRecord(
          thing.uri,
          Seq(Some(thing.uri), thing.identifier).flatten,
          DateTime.now().withZone(DateTimeZone.UTC),
          None, // lastChangedAt
          thing.title,
          Some(inDataset),
          None, // TODO isPartOf
          thing.subjects.map(Category(_)),
          thing.description.map(d => Seq(Description(d))).getOrElse(Seq.empty[Description]),
          thing.homepage,
          None, // license
          thing.languages.flatMap(Language.safeParse),
          thing.depictions.map(img => Depiction(img.uri, None, img.iiifEndpoint, None, None, None, None)),
          None, // TODO geometry
          None, // TODO representative point
          thing.temporal.map(convertPeriodOfTime),
          Seq.empty[Name],
          Seq.empty[String], // closeMatches
          Seq.empty[String]) // exactMatches

        (record, references)
      }
    }

    { stream: InputStream =>
      Scalagios.readAnnotations(stream, filename).flatMap(convertAnnotatedThing).toSeq }
  }

}
