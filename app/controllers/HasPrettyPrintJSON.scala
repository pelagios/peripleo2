package controllers

import play.api.mvc.{ AnyContent, Controller, Request }
import play.api.libs.json.{ Json, JsValue }
import scala.util.Try

trait HasPrettyPrintJSON { self: Controller =>
  
  protected def jsonOk(obj: JsValue)(implicit request: Request[AnyContent]) = {
    val pretty = Try(request.queryString.get("pretty").map(_.head.toBoolean).getOrElse(false)).getOrElse(false)
    if (pretty) Ok(Json.prettyPrint(obj)).as("application/json") else Ok(obj)
  }
  
}