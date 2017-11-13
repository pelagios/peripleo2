package services.visit

import play.api.libs.json._
import play.api.libs.functional.syntax._

object VisitType extends Enumeration {
  
  val EMBED      = Value("EMBED")
  val PAGE_VIEW  = Value("PAGE_VIEW")
  val SEARCH     = Value("SEARCH")
  val SELECTION  = Value("SELECTION")
  val VALIDATION = Value("VALIDATION") 
  
  implicit val visitTypeFormat: Format[VisitType.Value] =
    Format(
      JsPath.read[JsString].map(json => VisitType.withName(json.value)),
      Writes[VisitType.Value](t => Json.toJson(t.toString))
    )

}