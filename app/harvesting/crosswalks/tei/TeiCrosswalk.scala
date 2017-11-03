package harvesting.crosswalks.tei

import java.io.InputStream
import org.joda.time.DateTime
import scala.xml.{ Elem, Node, Text, XML }
import services.item._
import services.item.reference.{ UnboundReference, ReferenceQuote }
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
      Seq.empty[Link],
      None)
  }
  
  private def parseBody(identifier: String, baseURI: Option[String], xml: Elem): Seq[UnboundReference] = {
    
    // Returns a string attribute from the node, if it exists
    def getAttribute(attr: String, node: Node) = 
      node.attribute(attr).flatMap(_.headOption).map(_.text) 
      
    def toAnnotationLink(n: Option[String]) = { 
      val m = for {
        id <- n
        base <- baseURI
      } yield if (base.contains("recogito")) {
        Some(base + "/edit?annotation=" + id)
      } else {
        None
      }
      
      m.flatten
    }
          
    // Builds an UnboundReferences from a ref URI and text context
    def toReference(uri: String, homepage: Option[String], quote: ReferenceQuote) = UnboundReference(
        identifier,
        ItemRecord.normalizeURI(uri),
        None, // relation
        homepage,
        Some(quote),        
        None // Depiction
      )
   
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
    
    val references = flattened.sliding(2).foldLeft((Option.empty[Node], Seq.empty[UnboundReference])) { case ((maybePrev, refs), currentAndNext) =>
      val current = currentAndNext.head
      val next = currentAndNext(1)
      
      if (current.isInstanceOf[Text]) {
        // We only create a reference for entity nodes - skip this one
        (Some(current), refs)
      } else {
        // We know that this is an entity node - append prev and next iff they are text
        getAttribute("ref", current) match { 
          
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
            val chars = current.text
            val context = Seq(prefix, Some(chars), suffix).flatten.mkString("")
            val offset = prefix.map(_.size).getOrElse(0)
            
            val homepage = toAnnotationLink(getAttribute("n", current))
            val quote = ReferenceQuote(chars, Some(context), Some(offset))
            
            (Some(current), refs :+ toReference(entityURI, homepage, quote))
            
          // This is an entity node, but without a "ref" attribute - skip
          case None =>
            (Some(current), refs)
        }

      }
      
    }._2
    
    // Sliding window stops when it reaches the end of the list, whereas we need it to go one step further
    // i.e. when the *start* of the window is at the last position in the list - append this last step here
    if (flattened.size > 1) {
      val lastPair = flattened.takeRight(2)
      val (prefix, current) = (lastPair(0), lastPair(1))
      val maybeRef = getAttribute("ref", current)      
      val maybeHomepage = toAnnotationLink(getAttribute("n", current))
      
      if (current.isInstanceOf[Text] || maybeRef.isEmpty) {
        // Last node is a text node, or an entity node without a ref - nothing to do
        references
      } else if (prefix.isInstanceOf[Text]) {
        // Last node is an entity with a ref attribute, and prefix is text
        val quote = ReferenceQuote(current.text, Some(prefix.text + current.text), Some(prefix.text.size))
        references :+ toReference(maybeRef.get, maybeHomepage, quote)
      } else {
        // Last node is an entity with a ref, prefix is also an entity
        references
      }
    } else {
      references  
    }
  }
  
  private[tei] def parseTEIXML(identifier: String, dataset: PathHierarchy, stream: InputStream): (ItemRecord, Seq[UnboundReference]) = {
    val teiXml = XML.load(stream)        
    val record = parseHeader(identifier, dataset, teiXml)    
    val references = parseBody(identifier, record.homepage, teiXml)
    (record, references)    
  }
  
  def fromSingleFile(filename: String, dataset: PathHierarchy): InputStream => Seq[(ItemRecord, Seq[UnboundReference])] = { stream =>
    val identifier = dataset.ids.mkString("/") + filename.substring(0, filename.indexOf("."))  
    Seq(parseTEIXML(identifier, dataset, stream))
  }
  
}