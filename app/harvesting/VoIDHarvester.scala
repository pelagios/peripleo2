package harvesting

import akka.actor.ActorSystem
import akka.stream.Materializer
import harvesting.crosswalks._
import java.io.FileInputStream
import javax.inject.Inject
import org.pelagios.Scalagios
import org.pelagios.api.dataset.Dataset
import play.api.Logger
import play.api.libs.ws.WSClient
import play.api.libs.Files.TemporaryFile
import scala.concurrent.{ Future, ExecutionContext }
import services.item.{ ItemType, PathHierarchy }
import services.item.importers.AnnotationImporter
import services.task.TaskService

// TODO progress tracking that covers the entire process?

class VoIDHarvester @Inject() (
  val annotationImporter: AnnotationImporter,
  val taskService: TaskService,
  val ws: WSClient,
  implicit val materializer: Materializer, 
  implicit val system: ActorSystem,
  implicit val ctx: ExecutionContext
) extends HasFileDownload {
  
  private def parseVoID(file: TemporaryFile) = Future {
    scala.concurrent.blocking {
      Logger.info("Parsing VoID file " + file.file.getName)
      val in = new FileInputStream(file.file)
      val datasets = Scalagios.readVoID(in, file.file.getName)
      in.close()
      datasets
    }
  }
  
  /** Downloads the VoID and all referenced dumpfiles **/
  private def downloadFiles(voidURL: String) = {
    
    def fDownloadDatadumps(rootDatasets: Iterable[Dataset]) = {
      val datasets = rootDatasets.flatMap(PelagiosVoIDCrosswalk.flattenHierarchy).toSeq
      
      Future.sequence(datasets.map { dataset =>
        val uris = dataset.datadumps
        
        // TODO track progress via task service
        
        Future.sequence(uris.map { uri => 
          download(uri).map { file =>
            Some(file)
          } recover { case t: Throwable =>
            Logger.warn("Skipping failed download: " + t.getMessage)
            None
          }
        }) map { tmpFiles => (dataset, tmpFiles.flatten) }
      })
    }
    
    for {
      voidFile <- download(voidURL)
      rootDatasets <- parseVoID(voidFile)
      dumpfiles <- fDownloadDatadumps(rootDatasets)
    } yield (rootDatasets, dumpfiles)
  }

  /** Imports the datasets into the index **/
  private def importDatasets(rootDatasets: Iterable[Dataset]): Future[Boolean] = {
    val items = PelagiosVoIDCrosswalk.fromDatasets(rootDatasets.toSeq)
    annotationImporter.importDatasets(items)
  }
  
  /** Imports annotations from the dumpfiles into the index **/
  private def importAnnotationDumps(dumps: Seq[(Dataset, Seq[TemporaryFile])], username: String): Future[Boolean] = {
    val fImports = dumps.flatMap { case (dataset, tmpFiles) =>
      tmpFiles.map { tmp =>
        val parents = PelagiosVoIDCrosswalk.findParents(dataset).reverse :+ dataset
        val pathHierarchy = PathHierarchy(parents.map(d => (d.uri -> d.title)))
          
        val importer = new DumpImporter(taskService, ItemType.OBJECT)
        importer.importDump(
          "Importing Pelagios annotations from " + tmp.file.getName,
          tmp.file,
          tmp.file.getName,
          PelagiosAnnotationCrosswalk.fromRDF(tmp.file.getName, pathHierarchy),
          annotationImporter,
          username
          /* TODO job_id */)
      }
    }
    
    Future.sequence(fImports).map(successes =>
      !successes.exists(_ == false))
  }
  
  def harvest(voidURL: String, username: String) = {
    // TODO progress reporting - generate JOB ID, so we 
    // can aggregate sub-task progress later
    for { 
      (rootDatasets, dumpfiles) <- downloadFiles(voidURL)
      success1 <- importDatasets(rootDatasets)
      success2 <- importAnnotationDumps(dumpfiles, username)      
    } yield (success1 && success2)
  }
  
}
