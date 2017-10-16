package services.notification

import com.sksamuel.elastic4s.{ HitAs, RichSearchHit }
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.source.Indexable
import es.ES
import javax.inject.{ Inject, Singleton }
import play.api.libs.json.Json
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class NotificationService @Inject() (val es: ES, implicit val ctx: ExecutionContext) {
  
  implicit object NotificationIndexable extends Indexable[Notification] {
    override def json(n: Notification): String = Json.stringify(Json.toJson(n))
  }

  implicit object NotificationHitAs extends HitAs[Notification] {
    override def as(hit: RichSearchHit): Notification =
      Json.fromJson[Notification](Json.parse(hit.sourceAsString)).get
  }

  def insertNotification(notification: Notification): Future[Boolean] =
    es.client execute {
      index into ES.PERIPLEO / ES.NOTIFICATION source notification
    } map { 
      _ => true
    } recover { 
      case t: Throwable => false
    }
    
}
