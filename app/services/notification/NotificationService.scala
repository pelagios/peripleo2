package services.notification

import javax.inject.{ Inject, Singleton }
import es.ES
import scala.concurrent.ExecutionContextExecutor

@Singleton
class NotificationService @Inject() (val es: ES, implicit val ctx: ExecutionContextExecutor) {

  def insertNotification(notification: Notification) = {

  }

}
