package services.visit.info

import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.item.PathHierarchy

case class SelectionInfo(identifier: String, title: String, isInDataset: PathHierarchy)
  
/** Selection information for Visits of type SELECTION **/
object SelectionInfo {
  
  // Beep character to separate title and identifier
  val SEPARATOR = 0x0007.toChar
    
  implicit val selectionInfoReads: Reads[SelectionInfo] = (
    (JsPath \ "identifier").read[String] and
    (JsPath \ "title").read[String] and
    (JsPath \ "is_in_dataset").read[PathHierarchy] 
  )(SelectionInfo.apply _) 
  
  implicit val selectionInfoWrites: Writes[SelectionInfo] = (
    (JsPath \ "identifier").write[String] and
    (JsPath \ "title").write[String] and
    (JsPath \ "is_in_dataset").write[PathHierarchy] and
    (JsPath \ "identifier_title").write[String] // This is used only for more convenient aggregation
  )(s => (
    s.identifier,
    s.title,
    s.isInDataset,
    s.identifier + SEPARATOR + s.title))
  
}