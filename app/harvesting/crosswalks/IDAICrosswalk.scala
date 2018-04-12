package harvesting.crosswalks

import com.vividsolutions.jts.geom.{Coordinate, GeometryFactory}
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.util.Try
import services.item._
import services.HasNullableSeq

object IDAICrosswalk {
  
  val factory = new GeometryFactory()
  
  private def toItem(r: IDAIRecord, dataset: PathHierarchy): ItemRecord = {
    
    val coord = r.prefLocation.map { l =>
      new Coordinate(l.coordinates(0), l.coordinates(1))
    }
        
    ItemRecord(
      "http://gazetteer.dainst.org/place/" + r.id,
      Seq("http://gazetteer.dainst.org/place/" + r.id),
      DateTime.now,
      None, // lastChangedAt
      r.prefName.title,
      Some(dataset),
      None, // isPartOf
      r.types.map(Category(_)),
      Seq.empty[Description],
      None, // homepage
      None, // license
      Seq.empty[Language],
      Seq.empty[Depiction],
      coord.map(factory.createPoint(_)),
      coord,
      None, // temporalBounds
      r.names.map(n => Name(n.title, n.language.flatMap(Language.safeParse(_)))),
      r.links.map { link =>
        val linkType = link.predicate match {
          case "owl:sameAs" => LinkType.EXACT_MATCH
          case _ => LinkType.CLOSE_MATCH // just default to closeMatch
        }
        
        Link(link.uri, linkType)
      },
      Some("German Archaeological Institute"), // publisher
      None) // harvestVia
  }
  
  def fromJson(dataset: PathHierarchy)(record: String): Option[ItemRecord] =
    Try {
      Json.fromJson[IDAIRecord](Json.parse(record)) match {
        case s: JsSuccess[IDAIRecord] =>
          Some(toItem(s.get, dataset))
        case e: JsError =>
          play.api.Logger.error(e.toString)      
          None
      }
    }.toOption.flatten
  
}

case class IDAIRecord(
  id           : String,
  prefName     : IDAIName,
  names        : Seq[IDAIName],
  links        : Seq[IDAILink],
  types        : Seq[String],
  prefLocation : Option[IDAILocation],
  parent       : Option[String])
  
object IDAIRecord extends HasNullableSeq {
  
  implicit val idaiRecordReads: Reads[IDAIRecord] = (
    (JsPath \ "_id").read[String] and
    (JsPath \ "prefName").read[IDAIName] and
    (JsPath \ "names").read[Seq[IDAIName]] and
    (JsPath \ "links").read[Seq[IDAILink]] and
    (JsPath \ "types").readNullable[Seq[String]].map(fromOptSeq) and
    (JsPath \ "prefLocation").readNullable[IDAILocation] and
    (JsPath \ "parent").readNullable[String]
  )(IDAIRecord.apply _)
  
}

case class IDAILink(uri: String, predicate: String)

object IDAILink {
  
  implicit val idaiLinkReads: Reads[IDAILink] = (
    (JsPath \ "object").read[String] and
    (JsPath \ "predicate").read[String]
  )(IDAILink.apply _)
  
}

case class IDAIName(title: String, language: Option[String], ancient: Boolean, transliterated: Boolean)

object IDAIName {
  
  implicit val idaiNameReads: Reads[IDAIName] = (
    (JsPath \ "title").read[String] and
    (JsPath \ "language").readNullable[String] and
    (JsPath \ "ancient").readNullable[Boolean]
      .map(_.getOrElse(false)) and
    (JsPath \ "transliterated").readNullable[Boolean]
      .map(_.getOrElse(false))
  )(IDAIName.apply _)
}

case class IDAILocation(coordinates: Seq[Double], confidence: Int, publicSite: Boolean)

object IDAILocation {
  
  implicit val idaiLocationReads: Reads[IDAILocation] = (
    (JsPath \ "coordinates").read[Seq[Double]] and
    (JsPath \ "confidence").readNullable[Int].map(_.getOrElse(0)) and
    (JsPath \ "publicSite").readNullable[Boolean].map(_.getOrElse(false))
  )(IDAILocation.apply _)
  
}
