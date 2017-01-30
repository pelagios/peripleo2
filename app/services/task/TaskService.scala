package services.task

import com.sksamuel.elastic4s.{ HitAs, RichSearchHit }
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.source.Indexable
import javax.inject.{ Inject, Singleton }
import java.sql.Timestamp
import java.util.UUID
import play.api.Logger
import play.api.libs.json.Json
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.postfixOps
import services.ES

@Singleton
class TaskService @Inject() (val es: ES, implicit val ctx: ExecutionContext) {
  
  implicit object TaskIndexable extends Indexable[Task] {
    override def json(t: Task): String = Json.stringify(Json.toJson(t))
  }

  implicit object TaskHitAs extends HitAs[Task] {
    override def as(hit: RichSearchHit): Task =
      Json.fromJson[Task](Json.parse(hit.sourceAsString)).get
  }
  
  private def insertOrUpdateTask(task: Task): Future[Boolean] =
    es.client execute {
      update id task.id in ES.PERIPLEO / ES.TASK source task docAsUpsert
    } map { _ => 
      true
    } recover { case t: Throwable =>
      Logger.error(s"Error creating task record: " + t.getMessage)
      t.printStackTrace
      false
    }
    
  /*
  def updateProgress(uuid: UUID, progress: Int): Future[Unit] = db.withTransaction { sql => 
    sql.update(TASK)
      .set[Integer](TASK.PROGRESS, progress)
      .where(TASK.ID.equal(uuid))
      .execute()
  }
    
  def updateStatus(uuid: UUID, status: TaskStatus.Value): Future[Unit] = db.withTransaction { sql => 
    sql.update(TASK)
      .set(TASK.STATUS, status.toString)
      .where(TASK.ID.equal(uuid))
      .execute()
  }
  
  def updateStatusAndProgress(uuid: UUID, status: TaskStatus.Value, progress: Int): Future[Unit] = db.withTransaction { sql =>
    sql.update(TASK)
      .set(TASK.STATUS, status.toString)
      .set[Integer](TASK.PROGRESS, progress)
      .where(TASK.ID.equal(uuid))
      .execute()
  }
  
  def setCompleted(uuid: UUID, completedWith: Option[String] = None): Future[Unit] = db.withTransaction { sql =>
    sql.update(TASK)
      .set(TASK.STATUS, TaskStatus.COMPLETED.toString)
      .set(TASK.STOPPED_AT, new Timestamp(System.currentTimeMillis))
      .set(TASK.STOPPED_WITH, optString(completedWith))
      .set[Integer](TASK.PROGRESS, 100)
      .where(TASK.ID.equal(uuid))
      .execute()
  }
  
  def setFailed(uuid: UUID, failedWith: Option[String] = None): Future[Unit] = db.withTransaction { sql =>
    sql.update(TASK)
      .set(TASK.STATUS, TaskStatus.FAILED.toString)
      .set(TASK.STOPPED_AT, new Timestamp(System.currentTimeMillis))
      .set(TASK.STOPPED_WITH, optString(failedWith))
      .where(TASK.ID.equal(uuid))
      .execute()
  }
  
  def insertTask(
      taskType: TaskType,
      className: String,
      documentId: Option[String],
      filepartId: Option[UUID],
      spawnedBy: Option[String]
    ): Future[UUID] = db.withTransaction { sql =>
      
    val uuid = UUID.randomUUID
    
    val taskRecord = new TaskRecord(
      uuid,
      taskType.toString,
      className,
      optString(documentId),
      filepartId.getOrElse(null),
      optString(spawnedBy),
      new Timestamp(System.currentTimeMillis),
      null, // stopped_at
      null, // stopped_with
      TaskStatus.PENDING.toString,
      0
    )
    
    sql.insertInto(TASK).set(taskRecord).execute()
    
    uuid
  }
  */

}
