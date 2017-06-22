package harvesting.crosswalks.tei

import java.io.InputStream
import org.joda.time.DateTime
import scala.xml.{ Elem, Node, Text, XML }
import services.item._
import services.item.reference.{ UnboundReference, ReferenceType }

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
   
    val body = (xml \\ "body")(0)
    val placeNameNodes = body \\ "placeName"
    
    // Helper to clean up text extracted from XML
    def trim(str: String): String = str.replaceAll("\\s\\s+", " ").trim
    
    def getTextBetween(from: Option[Node], to: Option[Node]): Option[String] = {
      val allChildren = body.descendant
      
      val fromIdx = from.map { node =>
        // Skip placeName node, plus all its children
        val idx = allChildren.indexOf(node)
        val innerNodes = node.descendant
        (idx + innerNodes.size + 1)
      }.getOrElse(0)
      
      val toIdx = to.map(allChildren.indexOf(_)).getOrElse(allChildren.size)
      
      val text = allChildren.slice(fromIdx, toIdx).filter(_.isInstanceOf[Text]).mkString(" ").trim
      if (text.size > 0) Some(trim(text))
      else None
    }
    
    placeNameNodes.zipWithIndex.flatMap { case (node, idx) =>
      node.attribute("ref").map(_.head.text).map { placeURI =>

        val previousNode = 
          if (idx > 0) Some(placeNameNodes(idx - 1))
          else None
        val prefix = getTextBetween(previousNode, Some(node))
          
        val nextNode = 
          if (idx < placeNameNodes.size - 1) Some(placeNameNodes(idx + 1))
          else None
        val suffix = getTextBetween(Some(node), nextNode)
        
        val context = Seq(prefix, Some(node.text), suffix).flatten.mkString(" ")

        UnboundReference(
          identifier,
          ReferenceType.PLACE,
          ItemRecord.normalizeURI(placeURI),
          None, // relation
          None, // homepage
          Some(context),
          None // Depiction
        )
      }
    }
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