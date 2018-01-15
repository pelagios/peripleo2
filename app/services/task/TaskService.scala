package services.task

import com.sksamuel.elastic4s.{Hit, HitReader, Indexable}
import com.sksamuel.elastic4s.ElasticDsl._
import es.ES
import javax.inject.{ Inject, Singleton }
import java.sql.Timestamp
import java.util.UUID
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json.Json
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.postfixOps
import services.{ HasDate, Page }

@Singleton
class TaskService @Inject() (val es: ES, implicit val ctx: ExecutionContext) extends HasDate {

  implicit object TaskIndexable extends Indexable[Task] {
    override def json(t: Task): String = Json.stringify(Json.toJson(t))
  }

  implicit object TaskHitAs extends HitReader[Task] {
    override def read(hit: Hit): Either[Throwable, Task] =
      Right(Json.fromJson[Task](Json.parse(hit.sourceAsString)).get)
  }

  def listAll(offset: Int = 0, limit: Int = 20): Future[Page[Task]] =
    es.client execute {
      search in ES.PERIPLEO / ES.TASK start offset limit limit
    } map { response =>
      Page(response.tookInMillis, response.totalHits, offset, limit, response.to[Task])
    }

  def findByType(taskType: TaskType, offset: Int = 0, limit: Int = 20): Future[Page[Task]] =
    es.client execute {
      search in ES.PERIPLEO / ES.TASK query filter { termQuery("task_type" -> taskType.toString) }
    } map { response =>
      Page(response.tookInMillis, response.totalHits, offset, limit, response.to[Task])
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

  def deleteById(uuid: UUID): Future[Boolean] =
    es.client execute {
      delete id uuid.toString from ES.PERIPLEO / ES.TASK
    } map { _ => true
    } recover { case t: Throwable => false }

  def insertTask(taskType: TaskType, classname: String, caption: String, spawnedBy: String, jobId: Option[UUID] = None): Future[UUID] = {
    val task = Task(
      UUID.randomUUID(),
      jobId,
      taskType,
      classname,
      caption,
      spawnedBy,
      DateTime.now(),
      None,
      None,
      TaskStatus.PENDING,
      0)

    es.client execute {
      update id task.id in ES.PERIPLEO / ES.TASK docAsUpsert task 
    } map { _ =>
      task.id
    }
  }

}
