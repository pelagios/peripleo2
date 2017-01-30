package controllers.admin

import akka.stream.{ ActorAttributes, ClosedShape, Materializer, Supervision }
import akka.stream.scaladsl._
import akka.util.ByteString
import java.io.InputStream
import scala.concurrent.Await
import scala.concurrent.duration._
import services.task.{ TaskService, TaskStatus }
import services.HasBatchImport
import services.HasBatchImport

class StreamImporter(taskService: TaskService, implicit val materializer: Materializer) {

  private val BATCH_SIZE = 200

  private val decider: Supervision.Decider = {
    case t: Throwable =>
      t.printStackTrace()
      Supervision.Stop
  }

  def importRecords[T](is: InputStream, crosswalk: String => Option[T], service : HasBatchImport[T], username: String) = {
    
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
      service.importBatch(toImport)
      // TODO how to best compute total progress?
    }

    val graph = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>

      import GraphDSL.Implicits._

      source ~> parser ~> importer

      ClosedShape
    }).withAttributes(ActorAttributes.supervisionStrategy(decider))

    graph.run()
    
    // TODO how do we know the service is finished?
  }

}
