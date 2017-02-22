package services.item.search

import org.elasticsearch.search.aggregations.bucket.terms.Terms
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import scala.collection.JavaConverters._
import org.elasticsearch.search.aggregations.bucket.histogram.InternalHistogram

case class Aggregation(name: String, buckets: Seq[(String, Long)])

object Aggregation {

  implicit val bucketWrites =
    Writes[Tuple2[String, Long]](t => Json.obj(t._1 -> t._2))
  
  implicit def aggregationWrites: Writes[Aggregation] = (
    (JsPath \ "name").write[String] and
    (JsPath \ "buckets").write[Seq[(String, Long)]]
  )(unlift(Aggregation.unapply))
  
  def parseTerms(terms: Terms) = {
    val name = terms.getName
    val buckets = terms.getBuckets.asScala.toSeq.map(bucket =>
      (bucket.getKey.toString, bucket.getDocCount))
    Aggregation(name, buckets)
  }
  
  def parseHistogram(histogram: InternalHistogram[InternalHistogram.Bucket], name: String) = {
    val buckets = histogram.getBuckets.asScala.toSeq.map(bucket =>
      (bucket.getKey.toString, bucket.getDocCount))
    Aggregation(name, buckets)
  }
  
}