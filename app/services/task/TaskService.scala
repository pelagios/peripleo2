package services.task

import com.sksamuel.elastic4s.{ HitAs, RichSearchHit }
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.source.Indexable
import javax.inject.{ Inject, Singleton }
import java.sql.Timestamp
import java.util.UUID
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json.Json
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.postfixOps
import services.{ ES, HasDate }

@Singleton
class TaskService @Inject() (val es: ES, implicit val ctx: ExecutionContext) extends HasDate {
  
  implicit object TaskIndexable extends Indexable[Task] {
    override def json(t: Task): String = Json.stringify(Json.toJson(t))
  }

  implicit object TaskHitAs extends HitAs[Task] {
    override def as(hit: RichSearchHit): Task =
      Json.fromJson[Task](Json.parse(hit.sourceAsString)).get
  }
      
  def findById(uuid: UUID): Future[Option[Task]] =
    es.client execute {
      get id uuid.toString from ES.PERIPLEO / ES.TASK
    } map { response => 
      if (response.isExists) {
        val source = Json.parse(response.sourceAsString)
        Some(Json.fromJson[Task](source).get)        
      } else {
        None
      }
    }
    
  private def updateFields(uuid: UUID, fields: (String, Any)*): Future[Boolean] =
    es.client execute {
      update id uuid.toString in ES.PERIPLEO / ES.TASK docAsUpsert fields  
    } map { _ => true 
    } recover { case t: Throwable => false }
    
  def updateProgress(uuid: UUID, progress: Int): Future[Boolean] =
    updateFields(uuid, "progress" -> progress)

  def updateStatus(uuid: UUID, status: TaskStatus.Value): Future[Boolean] =
    updateFields(uuid, "status" -> status.toString)
  
  def updateStatusAndProgress(uuid: UUID, status: TaskStatus.Value, progress: Int): Future[Boolean] =
    updateFields(uuid,
        "status" -> status.toString,
        "progress" -> progress
      )
  
  def setCompleted(uuid: UUID, completedWith: Option[String] = None): Future[Boolean] =
    completedWith match {
      case Some(message) => updateFields(uuid,
          "status" -> TaskStatus.COMPLETED.toString,
          "stopped_at" -> formatDate(DateTime.now()),
          "stopped_with" -> message,
          "progress" -> 100
        )
        
      case None => updateFields(uuid,
          "status" -> TaskStatus.COMPLETED.toString,
          "stopped_at" -> formatDate(DateTime.now()),
          "progress" -> 100
        )
    }

  def setFailed(uuid: UUID, failedWith: Option[String] = None): Future[Boolean] =
    failedWith match {
      case Some(message) => updateFields(uuid,
          "status" -> TaskStatus.FAILED.toString,
          "stopped_at" -> formatDate(DateTime.now()),
          "stopped_with" -> message
        )
        
      case None => updateFields(uuid,
          "status" -> TaskStatus.FAILED.toString,
          "stopped_at" -> formatDate(DateTime.now())
        )
    }
  
  def insertTask(taskType: TaskType, classname: String, spawnedBy: String): Future[UUID] = {
    val task = Task(
      UUID.randomUUID(),
      taskType,
      classname,
      spawnedBy,
      DateTime.now(),
      None,
      None,
      TaskStatus.PENDING,
      0)
      
    es.client execute {
      update id task.id in ES.PERIPLEO / ES.TASK source task docAsUpsert
    } map { _ =>
      task.id      
    }
  }
  
}
