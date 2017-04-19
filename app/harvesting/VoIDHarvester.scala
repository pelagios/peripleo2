package harvesting

import akka.actor.ActorSystem
import akka.stream.Materializer
import harvesting.crosswalks._
import harvesting.loaders.DumpLoader
import java.io.FileInputStream
import javax.inject.Inject
import org.pelagios.Scalagios
import org.pelagios.api.dataset.Dataset
import play.api.Logger
import play.api.libs.ws.WSClient
import play.api.libs.Files.TemporaryFile
import scala.concurrent.{ Future, ExecutionContext }
import services.item.{ ItemService, ItemType, PathHierarchy }
import services.item.importers.{ DatasetImporter, ItemImporter }
import services.task.{ TaskService, TaskType }

class VoIDHarvester @Inject() (
  val itemService: ItemService,
  val taskService: TaskService,
  val ws: WSClient,
  implicit val materializer: Materializer, 
  implicit val system: ActorSystem,
  implicit val ctx: ExecutionContext
) extends HasFileDownload {
  
  // Maximum number of datasets that are downloaded in parallel.
  // Note that datasets != dumpfiles (one dataset may have many dumpfiles).
  // But it's a start.
  private val MAX_PARALLEL_DATASETS = 2
  
  // Wait time between HTTP requests
  private val WAIT_TIME_MILLIS = 1000
  
  private val taskType = TaskType("DATASET_IMPORT")
  
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
      val chunked = datasets.grouped(MAX_PARALLEL_DATASETS)
      
      def downladBatch(batch: Seq[Dataset]): Future[Seq[(Dataset, Seq[TemporaryFile])]] = {
        Logger.info("Downloading next batch of " + MAX_PARALLEL_DATASETS + " datasets")
        Future.sequence(batch.map { dataset =>
          val uris = dataset.datadumps
          Future.sequence(uris.map { uri => 
            download(uri).map { file =>
              Thread.sleep(WAIT_TIME_MILLIS)
              Some(file)
            } recover { case t: Throwable =>
              Logger.warn("Skipping failed download: " + t.getMessage)
              None
            }
          }) map { tmpFiles => (dataset, tmpFiles.flatten) }
        })
      }
      
      chunked.foldLeft(Future.successful(Seq.empty[(Dataset, Seq[TemporaryFile])])) { case (f, batch) =>
        // We'll download one batch in parallel
        f.flatMap { allDownloads =>
          downladBatch(batch).map { thisBatch =>
            allDownloads ++ thisBatch
          }
        }
      }
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
    new DatasetImporter(itemService, ItemType.DATASET).importDatasets(items)
  }
  
  /** Imports annotations from the dumpfiles into the index **/
  private def importAnnotationDumps(dumps: Seq[(Dataset, Seq[TemporaryFile])], username: String): Future[Boolean] = {
    val importer = new ItemImporter(itemService, ItemType.OBJECT)
    val fImports = dumps.flatMap { case (dataset, tmpFiles) =>
      tmpFiles.map { tmp =>
        val parents = PelagiosVoIDCrosswalk.findParents(dataset).reverse :+ dataset
        val pathHierarchy = PathHierarchy(parents.map(d => (d.uri -> d.title)))
          
        val loader = new DumpLoader(taskService, taskType)
        loader.importDump(
          "Importing Pelagios annotations from " + tmp.file.getName,
          tmp.file,
          tmp.file.getName,
          PelagiosAnnotationCrosswalk.fromRDF(tmp.file.getName, pathHierarchy),
          importer,
          username
          /* TODO job_id */)
      }
    }
    
    Future.sequence(fImports).map { successes =>
      val failed = successes.filter(_ == false)
      if (failed.size > 0)
        Logger.error(failed.size + " dumps failed during import")
        
      !successes.exists(_ == false)
    }
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
