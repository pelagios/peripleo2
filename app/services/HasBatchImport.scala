package services

import scala.concurrent.Future
import services.task.TaskType

trait HasBatchImport[T] {
  
  def taskType: TaskType
  
  def importBatch(batch: Seq[T]): Future[Seq[T]]
  
}