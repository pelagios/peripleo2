package services.item.crosswalks

import java.io.InputStream
import org.pelagios.Scalagios
import org.pelagios.api.annotation.AnnotatedThing
import services.item.Item

object PelagiosAnnotationCrosswalk {
  
  def fromRDF(filename: String): InputStream => Seq[Item] = {
    
    // TODO implement!
    def convertAnnotatedThing(thing: AnnotatedThing): Item = ???
    
    { stream: InputStream =>
      Scalagios.readAnnotations(stream, filename).map(convertAnnotatedThing).toSeq }
  }
  
}