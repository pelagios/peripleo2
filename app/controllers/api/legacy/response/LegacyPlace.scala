package controllers.api.legacy.response

import com.vividsolutions.jts.geom.Geometry
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.{ HasNullableSeq, HasGeometry }
import services.item.{Item, PathHierarchy}

case class LegacyPlace(item: Item, referencedIn: Seq[(String, Long)], peripleoBaseUrl: String) {
  
  /** Shorthands **/
  
  val identifier = item.identifiers.head
  
  val description = item.isConflationOf.flatMap(_.descriptions.map(_.description)).head
  
  val names = item.isConflationOf.flatMap(_.names.flatMap(_.name.split(",").map(_.trim))).distinct
  
  val bounds = item.representativeGeometry.map(geom => LegacyGeoBounds.fromEnvelope(geom.getEnvelopeInternal))
  
  val matches = (item.identifiers ++ item.isConflationOf.flatMap(_.directMatches)) diff Seq(identifier)
    
  val references = referencedIn.map(ReferencedIn(_, identifier, peripleoBaseUrl))
  
}

object LegacyPlace extends HasNullableSeq with HasGeometry {
  
  implicit val legacyPlaceWrites: Writes[LegacyPlace] = (
    (JsPath \ "identifier").write[String] and
    (JsPath \ "title").write[String] and
    (JsPath \ "object_type").write[String] and
    (JsPath \ "description").write[String] and
    (JsPath \ "names").write[Seq[String]] and
    (JsPath \ "matches").write[Seq[String]] and
    (JsPath \ "geo_bounds").writeNullable[LegacyGeoBounds] and
    (JsPath \ "geometry").writeNullable[Geometry] and
    (JsPath \ "network").write[LegacyNetwork] and
    (JsPath \ "referenced_in").write[Seq[ReferencedIn]]
  )(p => (
      p.identifier,
      p.item.title,
      "Place",
      p.description,
      p.names,
      p.matches,
      p.bounds,
      p.item.representativeGeometry,
      LegacyNetwork.computeNetwork(p.item),
      p.references
  ))

}

case class ReferencedIn(private val t: (String, Long), placeURI: String, peripleoBaseUrl: String) {
  
  val (identifier, title) = {
    val idAndTitle = t._1.split(PathHierarchy.INNER_SEPARATOR)
    (idAndTitle(0), idAndTitle(1))
  }
  
  val count = t._2
  
}

object ReferencedIn {
  
  implicit val referencedInWrites: Writes[ReferencedIn] = (
    (JsPath \ "title").write[String] and
    (JsPath \ "identifier").write[String] and
    (JsPath \ "count").write[Long] and
    (JsPath \ "peripleo_url").write[String]
  )(r => (
      r.title,
      r.identifier,
      r.count,
      s"${r.peripleoBaseUrl}#referencing=${r.placeURI}&datasets=${r.identifier}&filters=true"
  ))
  
}

case class LegacyNetworkNode(uri: String, label: Option[String], source: Option[String], isInnerNode: Boolean)

object LegacyNetworkNode {

  implicit val legacyNetworkNodeWrites: Writes[LegacyNetworkNode] = (
    (JsPath \ "uri").write[String] and
    (JsPath \ "label").writeNullable[String] and
    (JsPath \ "source_gazetteer").writeNullable[String] and
    (JsPath \ "is_inner_node").write[Boolean]
  )(unlift(LegacyNetworkNode.unapply)) 
  
}

case class LegacyNetworkEdge(source: Int, target: Int, isInnerEdge: Boolean)

object LegacyNetworkEdge {
  
  implicit val legacyNetworkEdgeWrites: Writes[LegacyNetworkEdge] = (
    (JsPath \ "source").write[Int] and
    (JsPath \ "target").write[Int] and
    (JsPath \ "is_inner_edge").write[Boolean]
  )(unlift(LegacyNetworkEdge.unapply)) 
  
}

case class LegacyNetwork(edges: Seq[LegacyNetworkEdge], nodes: Seq[LegacyNetworkNode])

object LegacyNetwork {
  
  implicit val legacyNetworkWrites: Writes[LegacyNetwork] = (
    (JsPath \ "edges").write[Seq[LegacyNetworkEdge]] and
    (JsPath \ "nodes").write[Seq[LegacyNetworkNode]]
  )(unlift(LegacyNetwork.unapply))
  
  /** Network nodes and edges **/
  def computeNetwork(item: Item) = {
    val records = item.isConflationOf
    
    val links = records.flatMap { record =>
      val matches = record.links.map(_.uri)
      Seq.fill(matches.size)(record.uri).zip(matches)   
    }
    
    val nodes = (links.map(_._1) ++ links.map(_._2)).distinct.map(uri => { 
      // If the node is an indexed place, it's an inner node; otherwise we require > 1 links to the node
      val record = records.find(_.uri == uri)
      val isInner = record.isDefined || links.filter(_._2 == uri).size > 1
      LegacyNetworkNode(uri, record.map(_.title), None, isInner) 
    })
    
    val edges = links.map { case (sourceURI, targetURI) => {
      val source = nodes.find(_.uri == sourceURI).get
      val target = nodes.find(_.uri == targetURI).get
      LegacyNetworkEdge(nodes.indexOf(source), nodes.indexOf(target), source.isInnerNode && target.isInnerNode) 
    }}  

    LegacyNetwork(edges, nodes)
  }
  
}