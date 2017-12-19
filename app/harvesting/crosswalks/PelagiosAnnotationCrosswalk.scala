package harvesting.crosswalks

import java.io.InputStream
import java.util.UUID
import org.joda.time.{ DateTime, DateTimeZone }
import org.pelagios.Scalagios
import org.pelagios.api.TimeInterval
import org.pelagios.api.annotation.AnnotatedThing
import org.pelagios.api.dataset.Dataset
import play.api.Logger
import services.item._
import services.item.reference.UnboundReference
import scala.util.Try

object PelagiosAnnotationCrosswalk extends PelagiosCrosswalk {

  // Bit annoying that this is duplication with the Place crosswalk - but would
  // rather have those few lines of duplication than pollute the code with a
  // Pelagios-RDF-specific trait
  private def convertTimeInterval(period: TimeInterval): TemporalBounds = {
    val startDate = period.start
    val endDate = period.end.getOrElse(startDate)

    TemporalBounds(
      new DateTime(startDate).withZone(DateTimeZone.UTC),
      new DateTime(endDate).withZone(DateTimeZone.UTC))
  }
    
  def findDatasetByUri(uri: String, parents: Seq[Dataset]) = {
    val parentMatch = parents.find(_.uri == uri)
    if (parentMatch.isDefined) parentMatch
    else findSubsetRecursive(uri, parents.last)
  }

  /** Returns a flat list of all things below this thing in the hierarchy **/
  private def flattenThingHierarchy(thing: AnnotatedThing): Seq[AnnotatedThing] =
    if (thing.parts.isEmpty) thing.parts
    else thing.parts ++ thing.parts.flatMap(flattenThingHierarchy)
  
  private def convertAnnotatedThing(thing: AnnotatedThing, inDataset: PathHierarchy) = {
    val references = thing.annotations.flatMap { _.places.headOption.map { placeUri =>
      UnboundReference(
        thing.uri,
        ItemRecord.normalizeURI(placeUri),
        None, // relation
        None, // homepage
        None, // quote
        None  // depiction
      )
    }}
    
    val namedPeriods = thing.namedPeriods.map { uri =>
      UnboundReference(
        thing.uri,
        ItemRecord.normalizeURI(uri),
        None, None, None, None) 
    }
    
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
      thing.depictions.map { img => 
        if (img.iiifEndpoint.isDefined)
          Depiction(img.iiifEndpoint.get, DepictionType.IIIF)
        else
          Depiction(img.uri, DepictionType.IMAGE)
      },
      None, // TODO geometry
      None, // TODO representative point
      thing.timeInterval.map(convertTimeInterval),
      Seq.empty[Name],
      Seq.empty[Link],
      None, None)
      
    (record, references ++ namedPeriods)
  }

  def fromRDF(filename: String, inDataset: PathHierarchy): InputStream => Seq[(ItemRecord, Seq[UnboundReference])] = {
    
    def convert(rootThing: AnnotatedThing) = {
      val flattenedHierarchy = rootThing +: flattenThingHierarchy(rootThing)
      flattenedHierarchy.map { t =>
        convertAnnotatedThing(t, inDataset)
      } 
    }
    
    { stream: InputStream =>
      Scalagios.readAnnotations(stream, filename).flatMap(convert).toSeq }
  }
  
  def fromRDF(filename: String, parents: Seq[Dataset]): InputStream => Seq[(ItemRecord, Seq[UnboundReference])] = {
    
    def convert(rootThing: AnnotatedThing) = {
      val flattenedHierarchy = rootThing +: flattenThingHierarchy(rootThing)
      flattenedHierarchy.map { t =>
          val hierarchy = t.inDataset match {
            case None => parents
            case Some(uri) =>
              findDatasetByUri(uri, parents) match {
                case Some(dataset) =>
                  findParents(dataset) :+ dataset
                
                case None =>
                  Logger.error("Unknown URI in void:inDataset: $uri")
                  throw new RuntimeException
              }
            }
          
          convertAnnotatedThing(t, PathHierarchy(hierarchy.map(d => (d.uri -> d.title))))
      }  
    }
    
    { stream: InputStream =>
      Scalagios.readAnnotations(stream, filename).flatMap(convert).toSeq }
  }

}
