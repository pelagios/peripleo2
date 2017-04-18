package services.notification

import javax.inject.{ Inject, Singleton }
import scala.concurrent.ExecutionContextExecutor
import services.ES

@Singleton
class NotificationService @Inject() (val es: ES, implicit val ctx: ExecutionContextExecutor) {
  
  def insertNotification(notification: Notification) = {
    
  }
  
}