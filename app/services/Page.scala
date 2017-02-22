package services

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import services.item.search.Aggregation

case class Page[A](took: Long, total: Long, offset: Int, limit: Long, items: Seq[A], aggregations: Seq[Aggregation]) {

  def map[B](f: (A) => B): Page[B] =
    Page(took, total, offset, limit, items.map(f), aggregations)

  def zip[B](s: Seq[B]) =
    Page(took, total, offset, limit, items.zip(s), aggregations)

}

object Page extends HasNullableSeq {
  
  /** Helper to create an empty page **/
  def empty[A] = Page(0, 0, 0, Int.MaxValue, Seq.empty[A], Seq.empty[Aggregation])

  /** JSON serialization **/
  implicit def pageWrites[A](implicit fmt: Writes[A]): Writes[Page[A]] = (
    (JsPath \ "took").write[Long] and
    (JsPath \ "total").write[Long] and
    (JsPath \ "offset").write[Int] and
    (JsPath \ "limit").write[Long] and
    (JsPath \ "items").write[Seq[A]] and
    (JsPath \ "aggregations").writeNullable[Seq[Aggregation]]
      .contramap[Seq[Aggregation]](toOptSeq[Aggregation])
  )(unlift(Page.unapply[A]))
  
}