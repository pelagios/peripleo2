package services

import services.task.TaskType

trait HasBatchImport[T] {
  
  def taskType: TaskType
  
  def importBatch(batch: Seq[T]): Seq[T]
  
}