package harvesting

import akka.actor.ActorSystem
import java.io.FileInputStream
import javax.inject.Inject
import org.pelagios.Scalagios
import org.pelagios.api.dataset.Dataset
import play.api.Logger
import play.api.libs.Files.TemporaryFile
import scala.concurrent.{ Future, ExecutionContext }
import services.item.{ ItemService, PathHierarchy }
import services.item.crosswalks._
import services.task.TaskService

// TODO progress tracking that covers the entire process?

class VoIDHarvester @Inject() (
  downloader: FileDownloader,
  itemService: ItemService,
  taskService: TaskService,
  implicit val system: ActorSystem,
  implicit val ctx: ExecutionContext
) {
  
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
  def downloadFiles(voidURL: String) = {
    
    def fDownloadDatadumps(rootDatasets: Iterable[Dataset]) = {
      val datasets = rootDatasets.flatMap(PelagiosVoIDCrosswalk.flattenHierarchy).toSeq
      
      Future.sequence(datasets.map { dataset =>
        val uris = dataset.datadumps
        Future.sequence(uris.map { uri => 
          downloader.download(uri).map { file =>
            Some(file)
          } recover { case t: Throwable =>
            Logger.warn("Skipping failed download: " + t.getMessage)
            None
          }
        }) map { tmpFiles => (dataset, tmpFiles.flatten) }
      })
    }
    
    for {
      voidFile <- downloader.download(voidURL)
      rootDatasets <- parseVoID(voidFile)
      dumpfiles <- fDownloadDatadumps(rootDatasets)
    } yield (rootDatasets, dumpfiles)
  }

  /** Imports the datasets into the index **/
  def importDatasets(rootDatasets: Iterable[Dataset]): Future[Boolean] = {
    val items = PelagiosVoIDCrosswalk.fromDatasets(rootDatasets.toSeq)
    itemService.importBatch(items).map { _.size == 0 }
  }
  
  /** Imports annotations from the dumpfiles into the index **/
  def importAnnotationDumps(dumps: Seq[(Dataset, Seq[TemporaryFile])], username: String): Future[Boolean] = {
    val fImports = dumps.flatMap { case (dataset, tmpFiles) =>
      tmpFiles.map { tmp =>
        val pathHierarchy = 
          PathHierarchy(PelagiosVoIDCrosswalk.findParents(dataset).reverse :+ dataset.uri)
          
        val importer = new DumpImporter(taskService)
        importer.importDump(tmp.file, tmp.file.getName, PelagiosAnnotationCrosswalk.fromRDF(tmp.file.getName, pathHierarchy), itemService, username)
      }
    }
    
    Future.sequence(fImports).map(successes =>
      !successes.exists(_ == false))
  }
  
  def harvest(voidURL: String, username: String) =
    for { 
      (rootDatasets, dumpfiles) <- downloadFiles(voidURL)
      success1 <- importDatasets(rootDatasets)
      success2 <- importAnnotationDumps(dumpfiles, username)      
    } yield (success1 && success2)
  
}
