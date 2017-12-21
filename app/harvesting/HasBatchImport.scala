package harvesting

import play.api.Logger
import scala.concurrent.{ ExecutionContext, Future }
import services.task.TaskType

trait HasBatchImport[T] {
  
  private val MAX_RETRIES = 5 // Max times an update will be retried in case of failure
  
  private val BACKOFF_MS = 1000
    
  private def importRecords(records: Seq[T], retries: Int = MAX_RETRIES)(implicit ctx: ExecutionContext): Future[Seq[T]] =
    records.foldLeft(Future.successful(Seq.empty[T])) { case (f, record) =>
      f.flatMap { failed =>
        importRecord(record).map { success =>
          if (success) failed
          else record +: failed 
        }
      }
    } flatMap { failedRecords =>
      Logger.info(s"Imported ${records.size - failedRecords.size} records") 
      if (failedRecords.size > 0 && retries > 0) {
        Logger.warn(s"${failedRecords.size} gazetteer records failed to import - retrying")
        
        // Start first retry immediately and then increases wait time for each subsequent retry 
        val backoff = (MAX_RETRIES - retries) * BACKOFF_MS  
        if (backoff > 0) {
          Logger.info(s"Waiting... ${backoff}ms")
          Thread.sleep(backoff)
        }
        
        Logger.debug("Retrying now.")
        importRecords(failedRecords, retries - 1)
      } else {
        if (failedRecords.size > 0) {
          Logger.error(s"${failedRecords.size} gazetteer records failed without recovery")
          failedRecords.foreach(record =>  Logger.error(record.toString))
        } else {
          Logger.info("No failed imports")
        }
        Future.successful(failedRecords)
      }
    }
    
  def importBatch(batch: Seq[T])(implicit ctx: ExecutionContext): Future[Seq[T]] = importRecords(batch)

  protected def importRecord(record: T): Future[Boolean]

}