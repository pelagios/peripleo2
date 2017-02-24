package services

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class Page[A](took: Long, total: Long, offset: Int, limit: Long, items: Seq[A]) {

  def map[B](f: (A) => B): Page[B] =
    Page(took, total, offset, limit, items.map(f))

  def zip[B](s: Seq[B]) =
    Page(took, total, offset, limit, items.zip(s))

}

object Page extends HasNullableSeq {
  
  /** Helper to create an empty page **/
  def empty[A] = Page(0, 0, 0, Int.MaxValue, Seq.empty[A])

  /** JSON serialization **/
  implicit def pageWrites[A](implicit fmt: Writes[A]): Writes[Page[A]] = (
    (JsPath \ "took").write[Long] and
    (JsPath \ "total").write[Long] and
    (JsPath \ "offset").write[Int] and
    (JsPath \ "limit").write[Long] and
    (JsPath \ "items").write[Seq[A]]
  )(unlift(Page.unapply[A]))
  
}