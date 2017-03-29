package services.task

import java.util.UUID
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import services.HasDate

case class Task(
  id          : UUID,
  job         : Option[UUID],
  taskType    : TaskType,
  classname   : String,
  caption     : String,
  spawnedBy   : String,
  spawnedAt   : DateTime,
  stoppedAt   : Option[DateTime],
  stoppedWith : Option[String],
  status      : TaskStatus.Value,
  progress    : Int)

object Task extends HasDate {
  
  implicit val userFormat: Format[Task] = (
    (JsPath \ "id").format[UUID] and
    (JsPath \ "job").formatNullable[UUID] and
    (JsPath \ "task_type").format[TaskType] and
    (JsPath \ "classname").format[String] and
    (JsPath \ "caption").format[String] and
    (JsPath \ "spawned_by").format[String] and
    (JsPath \ "spawned_at").format[DateTime] and
    (JsPath \ "stopped_at").formatNullable[DateTime] and
    (JsPath \ "stopped_with").formatNullable[String] and
    (JsPath \ "status").format[TaskStatus.Value] and
    (JsPath \ "progress").format[Int]
  )(Task.apply, unlift(Task.unapply))
  
}

case class TaskType(private val name: String) {
  
  override def toString = name
  
}

object TaskType {
  
  implicit val taskTypeFormat: Format[TaskType] =
    Format(
      JsPath.read[JsString].map(json => TaskType(json.value)),
      Writes[TaskType](t => Json.toJson(t.name))
    )
  
}

object TaskStatus extends Enumeration {

  val PENDING = Value("PENDING")

  val RUNNING = Value("RUNNING")
  
  val COMPLETED = Value("COMPLETED")
  
  val FAILED = Value("FAILED")
  
  implicit val taskStatusFormat: Format[TaskStatus.Value] =
    Format(
      JsPath.read[JsString].map(json => TaskStatus.withName(json.value)),
      Writes[TaskStatus.Value](s => Json.toJson(s.toString))
    )

}
