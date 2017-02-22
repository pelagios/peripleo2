package controllers.admin

import akka.Done
import akka.stream.{ ActorAttributes, ClosedShape, Materializer, Supervision }
import akka.stream.scaladsl._
import akka.util.ByteString
import java.io.InputStream
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration._
import services.HasBatchImport
import services.task.{ TaskService, TaskStatus }


class StreamImporter(taskService: TaskService, implicit val materializer: Materializer) {

  private val BATCH_SIZE = 200

  private val decider: Supervision.Decider = {
    case t: Throwable =>
      t.printStackTrace()
      Supervision.Stop
  }
  
  def importRecords[T](is: InputStream, crosswalk: String => Option[T], service : HasBatchImport[T], username: String)(implicit ctx: ExecutionContext) = {
    
    val taskId = Await.result(taskService.insertTask(service.taskType, service.getClass.getName, username), 10.seconds)
    taskService.updateStatus(taskId, TaskStatus.RUNNING)

    val source = StreamConverters.fromInputStream(() => is, 1024)
      .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = Int.MaxValue, allowTruncation = false))
      .map(_.utf8String)

    val parser = Flow.fromFunction[String, Option[T]](crosswalk)
      .withAttributes(ActorAttributes.supervisionStrategy(decider))
      .grouped(BATCH_SIZE)

    val importer = Sink.foreach[Seq[Option[T]]] { records =>
      val toImport = records.flatten
      Await.result(service.importBatch(toImport), 10.minutes)
      
      // TODO how to best compute total progress?
      
    }

    val graph = RunnableGraph.fromGraph(GraphDSL.create(importer) { implicit builder => sink =>

      import GraphDSL.Implicits._

      source ~> parser ~> sink

      ClosedShape
    }).withAttributes(ActorAttributes.supervisionStrategy(decider))
 
    graph.run().map { _ =>
      taskService.setCompleted(taskId)
    } recoverWith { case t: Throwable =>
      taskService.setFailed(taskId, Some(t.getMessage))
    }    
  }

}
