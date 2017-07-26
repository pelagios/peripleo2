package harvesting.crosswalks.tei

import java.io.InputStream
import org.joda.time.DateTime
import play.api.Logger
import scala.xml.{ Elem, Node, Text, XML }
import services.item._
import services.item.reference.{ UnboundReference, ReferenceType }
import scala.collection.mutable.ListBuffer

object TeiCrosswalk {
  
  private def parseHeader(identifier: String, dataset:PathHierarchy, xml: Elem): ItemRecord = {     
    val title = (xml \\ "teiHeader" \\ "title").text
    val homepage = (xml \\ "teiHeader" \\ "sourceDesc" \\ "link").headOption.flatMap(_.attribute("target").map(_.text))
    
    ItemRecord(
      identifier,
      Seq(identifier),
      DateTime.now,
      None, // lastChangedAt
      title,
      Some(dataset),
      None, // isPartOf
      Seq.empty[Category],
      Seq.empty[Description],
      homepage,
      None, // license
      Seq.empty[Language],
      Seq.empty[Depiction],
      None, None, // geometry, representativePoint
      None, // temporalBounds
      Seq.empty[Name],
      Seq.empty[String], Seq.empty[String])
  }
  
  private def parseBody(identifier: String, xml: Elem): Seq[UnboundReference] = {
   
    // Flattens the node to a list of text and placeName tags
    def flattenNode(node: Node, flattened: Seq[Node] = Seq.empty[Node]): Seq[Node] = {      
      if (node.child.isEmpty || node.label == "placeName")
        flattened :+ node
      else
        flattened ++ node.child.flatMap(n => flattenNode(n, flattened))
    }

    val flattened =
      // 'Flatten' the contents of the body to a linear list of texts and entity tags
      flattenNode((xml \\ "body")(0))
      // Merge consecutive text nodes
      .foldLeft(Seq.empty[Node]) { case (list, node) =>
        list.lastOption match {
          // First iteration - new list
          case None => Seq(node)

          // Previous node was a text node
          case Some(previous) if previous.isInstanceOf[Text] =>
            if (node.isInstanceOf[Text]) {
              list.dropRight(1) :+ Text(previous.text + node.text)
            } else {
              list :+ node
            }
            
          // Previous node was an entity node - append
          case _ => list :+ node
        }
      }
    
    flattened.sliding(2).foldLeft((Option.empty[Node], Seq.empty[UnboundReference])) { case ((maybePrev, refs), currentAndNext) =>
      val current = currentAndNext.head
      val next = currentAndNext(1)
      
      if (current.isInstanceOf[Text]) {
        // We only create a reference for entity nodes - skip this one
        (Some(current), refs)
      } else {
        // We know that this is an entity node - append prev and next iff they are text
        current.attribute("ref").flatMap(_.headOption).map(_.text) match { 
          
          case Some(entityURI) =>
            // The text before this entity
            val prefix = maybePrev match {
              case Some(previous) if previous.isInstanceOf[Text] => Some(previous.text)
              case _ => None
            }
            
            // The text after this entity
            val suffix = 
              if (next.isInstanceOf[Text]) Some(next.text)
              else None
              
            // Concatenated text context
            val context = Seq(prefix, Some(current.text), suffix).flatten.mkString("")     
            
            val reference = UnboundReference(
              identifier,
              ReferenceType.PLACE,
              ItemRecord.normalizeURI(entityURI),
              None, // relation
              None, // homepage
              Some(context),
              None // Depiction
            )
    
            (Some(current), refs :+ reference)
            
          // This is an entity node, but without a "ref" attribute - skip
          case None =>
            (Some(current), refs)
            
        }
        

      }
      
    }._2
  }
  
  private[tei] def parseTEIXML(identifier: String, dataset: PathHierarchy, stream: InputStream): (ItemRecord, Seq[UnboundReference]) = {
    val teiXml = XML.load(stream)        
    val record = parseHeader(identifier, dataset, teiXml)    
    val references = parseBody(identifier, teiXml)
    (record, references)    
  }
  
  def fromSingleFile(filename: String, dataset: PathHierarchy): InputStream => Seq[(ItemRecord, Seq[UnboundReference])] = { stream =>
    val identifier = dataset.ids.mkString("/") + filename.substring(0, filename.indexOf("."))  
    Seq(parseTEIXML(identifier, dataset, stream))
  }
  
}