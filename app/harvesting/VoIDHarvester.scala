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
import play.api.libs.Files
import play.api.libs.Files.{TemporaryFile, TemporaryFileCreator}
import scala.concurrent.{Future, ExecutionContext}
import services.item.{ItemService, ItemType, PathHierarchy}
import services.item.importers.{DatasetImporter, ItemImporter}
import services.task.{TaskService, TaskType}

class VoIDHarvester @Inject() (
  val itemService: ItemService,
  val taskService: TaskService,
  val ws: WSClient,
  implicit val tmpCreator: TemporaryFileCreator,
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
  
  private def parseVoID(tmp: TemporaryFile) = Future {
    scala.concurrent.blocking {
      Logger.info("Parsing VoID file " + tmp.path.getFileName)
      val in = new FileInputStream(tmp.path.toFile)
      val datasets = Scalagios.readVoID(in, tmp.path.getFileName.toString)
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
        Future.sequence(batch.map { dataset =>
          dataset.datadumps.foldLeft(Future.successful(Seq.empty[Files.TemporaryFile])) { case (f, uri) =>
            f.flatMap { results =>
              download(uri).map { file =>
                Thread.sleep(WAIT_TIME_MILLIS)
                results :+ file
              } recover { case t: Throwable =>
                Logger.warn("Skipping failed download: " + t.getMessage)
                results
              }
            }
          } map { (dataset, _) }
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
  private def importDatasets(rootDatasets: Iterable[Dataset], voidURL: String): Future[Boolean] = {
    val items = PelagiosVoIDCrosswalk.fromDatasets(rootDatasets.toSeq, voidURL)
    new DatasetImporter(itemService, ItemType.DATASET.ANNOTATIONS).importDatasets(items)
  }
  
  /** Imports annotations from the dumpfiles into the index **/
  private def importAnnotationDumps(dumps: Seq[(Dataset, Seq[TemporaryFile])], username: String): Future[Boolean] = {
    val importer = new ItemImporter(itemService, ItemType.OBJECT)
    val fImports = dumps.flatMap { case (dataset, tmpFiles) =>
      tmpFiles.map { tmp =>
        val parents = PelagiosVoIDCrosswalk.findParents(dataset).reverse :+ dataset
          
        val loader = new DumpLoader(taskService, taskType)
        loader.importDump(
          s"Importing Pelagios annotations from ${tmp.path.getFileName}",
          tmp.path.toFile,
          tmp.path.getFileName.toString,
          PelagiosAnnotationCrosswalk.fromRDF(tmp.path.getFileName.toString, parents),
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
      success1 <- importDatasets(rootDatasets, voidURL)
      success2 <- importAnnotationDumps(dumpfiles, username)      
    } yield (success1 && success2)
  }
  
}
