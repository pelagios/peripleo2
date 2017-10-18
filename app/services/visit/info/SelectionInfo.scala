package services.visit.info

import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.item.PathHierarchy

case class SelectionInfo(identifier: String, title: String, isInDataset: PathHierarchy)
  
/** Selection information for Visits of type SELECTION **/
object SelectionInfo {
  
  implicit val selectionInfoFormat: Format[SelectionInfo] = (
    (JsPath \ "identifier").format[String] and
    (JsPath \ "title").format[String] and
    (JsPath \ "is_in_dataset").format[PathHierarchy]
  )(SelectionInfo.apply, unlift(SelectionInfo.unapply)) 
  
}