package controllers.admin

import java.io.{ File, FileInputStream, InputStream }
import java.util.zip.GZIPInputStream
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration._
import services.HasBatchImport
import services.task.{ TaskService, TaskStatus }

class DumpImporter(taskService: TaskService) {
  
  private val MAX_BATCHES = 20
  
  private def getStream(file: File, filename: String) =
    if (filename.endsWith(".gz"))
      new GZIPInputStream(new FileInputStream(file))
    else
      new FileInputStream(file)
  
  def importDump[T](file: File, filename: String, crosswalk: InputStream => Seq[T], service : HasBatchImport[T], username: String)(implicit ctx: ExecutionContext) = {
      
    def split[T](records: Seq[T], n: Int): Seq[Seq[T]] =
      if (records.size <= n) {
        // Fewer (or equal) records than requested batches - create batches with one record each
        records.map(Seq(_))
      } else {
        val batchSize = Math.ceil(records.size.toDouble / n).toInt
        records.grouped(batchSize).toSeq
      }
    
    /*
    def importBatch(batch: Seq[T], service: HasBatchImport[T]) = batch.foldLeft(Future.successful(Seq.empty[T])) { case (f, record) =>
      f.flatMap { failedRecords =>
        service.importBatch(batch)
      }
    }
    */
      
    val taskId = Await.result(taskService.insertTask(service.taskType, service.getClass.getName, username), 10.seconds)
    taskService.updateStatus(taskId, TaskStatus.RUNNING)
    
    // TODO this is now a Future[Future[...]] - clean up
    
    Future {
      scala.concurrent.blocking {
        val records = crosswalk(getStream(file, filename))
        val batches = split(records, MAX_BATCHES)
        val increment = 100.0 / batches.size
        
        val fImport = batches.zipWithIndex.foldLeft(Future.successful(Seq.empty[T])) { case (f, (batch, idx)) =>
          f.flatMap { unrecoverable =>
            service.importBatch(batch).flatMap { failed =>
              val progress = Math.ceil((idx + 1) * increment).toInt
              taskService.updateProgress(taskId, progress).map(_ => unrecoverable ++ failed)
            }
          }
        }
        
        fImport.flatMap { unrecoverable =>
          val message = if (unrecoverable.isEmpty) None else Some("Failed to import " + unrecoverable.size + " records")
          taskService.setCompleted(taskId)
        } recover { case t: Throwable =>
          taskService.setFailed(taskId, Some(t.getMessage))
        }
      }
    }
  }
  
}
