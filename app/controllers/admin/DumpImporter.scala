package controllers.admin

import akka.actor.ActorSystem
import java.io.{ File, FileInputStream, InputStream }
import java.util.UUID
import java.util.zip.GZIPInputStream
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration._
import services.HasBatchImport
import services.task.{ TaskService, TaskStatus }

class DumpImporter(taskService: TaskService) extends BaseImporter {

  private val MAX_BATCHES = 20
  
  private def split[T](records: Seq[T], n: Int): Seq[Seq[T]] =
    if (records.size <= n) {
      // Fewer (or equal) records than requested batches - create batches with one record each
      records.map(Seq(_))
    } else {
      val batchSize = Math.ceil(records.size.toDouble / n).toInt
      records.grouped(batchSize).toSeq
    }

  def importDump[T](file: File, filename: String, crosswalk: InputStream => Seq[T], 
      service : HasBatchImport[T], username: String)(implicit ctx: ExecutionContext, system: ActorSystem): Future[Boolean] = {
    
    val taskId = Await.result(taskService.insertTask(service.taskType, service.getClass.getName, username), 10.seconds)
    taskService.updateStatus(taskId, TaskStatus.RUNNING)
    
    val fConvert: Future[Seq[T]] = Future {
      // This is a long-running operation
      scala.concurrent.blocking { crosswalk(getStream(file, filename)) }
    }
    
    def fImport(records: Seq[T]): Future[Seq[T]] = {
      val batches = split(records, MAX_BATCHES)
      val increment = 100.0 / batches.size
        
      batches.zipWithIndex.foldLeft(Future.successful(Seq.empty[T])) { case (f, (batch, idx)) =>
        f.flatMap { unrecoverable =>
          service.importBatch(batch).flatMap { failed =>
            val progress = Math.ceil((idx + 1) * increment).toInt
            taskService.updateProgress(taskId, progress).map(_ => unrecoverable ++ failed)
          }
        }
      }
    }
    
    val f = for {
      records <- fConvert
      unrecoverable <- fImport(records)
    } yield unrecoverable
    
    val fSuccess = f.flatMap { unrecoverable =>
      val message = if (unrecoverable.isEmpty) None else Some("Failed to import " + unrecoverable.size + " records")
      taskService.setCompleted(taskId, message).map { _ =>
        system.scheduler.scheduleOnce(1.minute)(taskService.deleteById(taskId))
        true
      }
    } recoverWith { case t: Throwable =>
      taskService.setFailed(taskId, Some(t.getMessage)).map { _ =>
        system.scheduler.scheduleOnce(1.minute)(taskService.deleteById(taskId))
        false
      }
    }

    fSuccess
  }

}
