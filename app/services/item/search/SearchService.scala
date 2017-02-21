package services.item.search

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ HitAs, RichSearchHit }
import javax.inject.{ Inject, Singleton }
import play.api.libs.json.Json
import scala.concurrent.{ Future, ExecutionContext }
import services.{ ES, Page }
import services.item.Item
  
@Singleton
class SearchService @Inject() (val es: ES, implicit val ctx: ExecutionContext) {

  implicit object ItemHitAs extends HitAs[Item] {
    override def as(hit: RichSearchHit): Item =
      Json.fromJson[Item](Json.parse(hit.sourceAsString)).get
  }
  
  def query(args: SearchArgs): Future[Page[Item]] = {
    es.client execute {
      args.query match {
        case Some(query) =>
          search in ES.PERIPLEO / ES.ITEM query query start args.offset limit args.limit
          
        case None =>
          search in ES.PERIPLEO / ES.ITEM start args.offset limit args.limit
      }
    } map { response =>
      val items = response.as[Item].toSeq 
      Page(response.tookInMillis, response.totalHits, args.offset, args.limit, items)  
    }     
  }
  
}