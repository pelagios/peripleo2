package services.item.place

import com.vividsolutions.jts.geom.{ Coordinate, Geometry }
import org.joda.time.{ DateTime, DateTimeZone }
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import services.{ HasDate, HasGeometry, HasNullableSeq }
import services.item.{ ItemType, Language, TemporalBounds, PathSegment }

case class Place (rootUri: String, isConflationOf: Seq[GazetteerRecord]) {
  
  lazy val uris: Seq[String] = isConflationOf.map(_.uri)

  lazy val sourceGazetteers: Seq[Gazetteer] = isConflationOf.map(_.sourceGazetteer)

  lazy val titles: Seq[String] = isConflationOf.map(_.title)
  
  lazy val languages: Seq[Language] = isConflationOf.flatMap(_.names.flatMap(_.language)).distinct
  
  lazy val temporalBoundsUnion: Option[TemporalBounds] = {
    val bounds = isConflationOf.flatMap(_.temporalBounds)
    if (bounds.isEmpty)
      None
    else
      Some(TemporalBounds.computeUnion(bounds))
  }
      
  // Descriptions as Map[description -> list of gazetteers including the description]
  lazy val descriptions =
    isConflationOf
      .flatMap(g => g.descriptions.map((_, g.sourceGazetteer)))
      .groupBy(_._1)
      .map { case (description, s) => (description -> s.map(_._2)) }.toMap
      
  // Names as Map[name -> list of gazetteers including the name]
  lazy val names =
    isConflationOf
      .flatMap(g => g.names.map((_, g.sourceGazetteer)))
      .groupBy(_._1)
      .map { case (name, s) => (name -> s.map(_._2)) }.toMap

  // Place types as Map[placeType -> list of gazetteers including the type]
  lazy val placeTypes =
    isConflationOf
      .flatMap(g => g.placeTypes.map((_, g.sourceGazetteer)))
      .groupBy(_._1)
      .map { case (placeType, s) => (placeType -> s.map(_._2)) }.toMap
      
  lazy val closeMatches = isConflationOf.flatMap(_.closeMatches)
  
  lazy val exactMatches = isConflationOf.flatMap(_.exactMatches)
  
  lazy val allMatches: Seq[String] = closeMatches ++ exactMatches
  
  lazy val completionData = 
    CompletionData(
      titles ++ names.map(_._1.name),
      rootUri,
      ItemType.PLACE,
      titles.head,
      descriptions.map(_._1.description).headOption)

}

private[item] case class CompletionData private(input: Seq[String], output: String, payload: CompletionData.Payload)

private[item] object CompletionData {
  
  case class Payload(itemId: String, itemType: ItemType.Value, description: Option[String])
  
  def apply(input: Seq[String], itemId: String, itemType: ItemType.Value, title: String,
    description : Option[String]): CompletionData = 
      CompletionData(input, title, Payload(itemId, itemType, description))
   
  // https://www.elastic.co/guide/en/elasticsearch/reference/2.4/search-suggesters-completion.html#indexing
  implicit val payloadWrites: Writes[Payload] = (
    (JsPath \ "id").write[String] and
    (JsPath \ "type").write[ItemType.Value] and
    (JsPath \ "description").writeNullable[String]
  )(unlift(Payload.unapply))

  implicit val completionDataWrites: Writes[CompletionData] = (
    (JsPath \ "input").write[Seq[String]] and
    (JsPath \ "output").write[String] and
    (JsPath \ "payload").write[Payload]
  )(unlift(CompletionData.unapply))
  
}

object Place extends HasGeometry with HasNullableSeq {
  
  // A serialized place is also a serialized item, therefore reading and writing needs to be 'asymmetric' 
  implicit val placeReads: Reads[Place] = (
    (JsPath \ "root_uri").read[String] and
    (JsPath \ "is_conflation_of").read[Seq[GazetteerRecord]]
  )(Place.apply _)
        
  implicit val placeWrites: Writes[Place] = (
    (JsPath \ "root_uri").write[String] and
    (JsPath \ "identifiers").write[Seq[String]] and
    (JsPath \ "item_type").write[ItemType.Value] and
    (JsPath \ "title").write[String] and
    (JsPath \ "is_in_dataset").write[Seq[String]] and
    (JsPath \ "languages").writeNullable[Seq[Language]] and
    (JsPath \ "temporal_bounds").writeNullable[TemporalBounds] and
    (JsPath \ "is_conflation_of").write[Seq[GazetteerRecord]] and
    (JsPath \ "suggest").write[CompletionData]
  )(place => (
      place.rootUri,
      place.uris,
      ItemType.PLACE,
      place.titles.head,
      place.sourceGazetteers.map(gazetteer =>
        PathSegment(gazetteer.name, gazetteer.name).toString),
      toOptSeq(place.languages),
      place.temporalBoundsUnion,
      place.isConflationOf,
      place.completionData)
  )
  
}