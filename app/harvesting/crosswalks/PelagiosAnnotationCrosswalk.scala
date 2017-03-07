package harvesting.crosswalks

import java.io.InputStream
import java.util.UUID
import org.joda.time.{ DateTime, DateTimeZone }
import org.pelagios.Scalagios
import org.pelagios.api.PeriodOfTime
import org.pelagios.api.annotation.AnnotatedThing
import services.item._
import services.item.place.GazetteerRecord

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

  def fromRDF(filename: String, inDataset: PathHierarchy): InputStream => Seq[(Item, Seq[Reference])] = {

    def convertAnnotatedThing(thing: AnnotatedThing): Seq[(Item, Seq[Reference])] = {
      val flattenedHierarchy = thing +: flattenThingHierarchy(thing)
      flattenedHierarchy.map { thing =>
        
        val references = thing.annotations.flatMap { _.places.headOption.map { placeUri =>
          Reference(
            ReferenceType.PLACE,
            None, // TODO relation
            GazetteerRecord.normalizeURI(placeUri),
            None, // homepage
            None, // context
            None  // depiction
          )
        }}
        
        val item =Item(
          Seq(Some(thing.uri), thing.identifier).flatten,
          ItemType.OBJECT,
          thing.title,
          Some(DateTime.now().withZone(DateTimeZone.UTC)),
          None, // last_changed_at
          thing.subjects.map(Category(_)),
          Seq(inDataset),
          None, // TODO is_part_of
          thing.description.map(d => Seq(Description(d))).getOrElse(Seq.empty[Description]),
          thing.homepage,
          None, // license
          thing.languages.map(Language(_)),
          None, // TODO geometry
          None, // TODO representative point
          thing.temporal.map(convertPeriodOfTime),
          Seq.empty[String], // periods
          thing.depictions.map(url => Depiction(url, None, None, None, None, None))
        )
        
        (item, references)
      }
    }
    
    { stream: InputStream =>
      Scalagios.readAnnotations(stream, filename).flatMap(convertAnnotatedThing).toSeq }
  }
  
}