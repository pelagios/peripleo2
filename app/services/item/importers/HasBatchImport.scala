package services.item.importers

import play.api.Logger
import scala.concurrent.{ ExecutionContext, Future }
import services.item.ItemType
import services.task.TaskType

trait HasBatchImport[T] {
  
  def TASK_TYPE: TaskType
  
  private def MAX_RETRIES = 5 // Max times an update will be retried in case of failure
    
  private def importRecords(records: Seq[T], itemType: ItemType, retries: Int = MAX_RETRIES)(implicit ctx: ExecutionContext): Future[Seq[T]] =
    records.foldLeft(Future.successful(Seq.empty[T])) { case (f, record) =>
      f.flatMap { failed =>
        importRecord(record, itemType).map { success =>
          if (success) failed
          else record +: failed 
        }
      }
    } flatMap { failedRecords =>
      Logger.info("Imported " + (records.size - failedRecords.size) + " records") 
      if (failedRecords.size > 0 && retries > 0) {
        Logger.warn(failedRecords.size + " gazetteer records failed to import - retrying")
        importRecords(failedRecords, itemType, retries - 1)
      } else {
        if (failedRecords.size > 0) Logger.error(failedRecords.size + " gazetteer records failed without recovery")
        else Logger.info("No failed imports")
        Future.successful(failedRecords)
      }
    }
    
  def importBatch(batch: Seq[T], itemType : ItemType)(implicit ctx: ExecutionContext): Future[Seq[T]] = importRecords(batch, itemType)

  protected def importRecord(record: T, itemType: ItemType): Future[Boolean]

}