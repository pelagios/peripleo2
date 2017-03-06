package harvesting

import java.io.FileInputStream
import javax.inject.Inject
import org.pelagios.Scalagios
import org.pelagios.api.dataset.Dataset
import play.api.Logger
import play.api.libs.Files.TemporaryFile
import scala.concurrent.{ Future, ExecutionContext }


class VoIDHarvester @Inject() (downloader: FileDownloader, implicit val ctx: ExecutionContext) {
  
  private def flattenHierarchy(rootset: Dataset): Seq[Dataset] =
    if (rootset.subsets.isEmpty) Seq(rootset)
    else rootset +: rootset.subsets.flatMap(flattenHierarchy)
  
  private def parseVoID(file: TemporaryFile) = Future {
    scala.concurrent.blocking {
      Logger.info("Parsing VoID file " + file.file.getName)
      val in = new FileInputStream(file.file)
      val datasets = Scalagios.readVoID(in, file.file.getName)
      in.close()
      datasets
    }
  }
  
  def harvest(voidURL: String) = {
    
    def fHarvestDumpfiles(rootDatasets: Iterable[Dataset]) = {
      val uris = rootDatasets.flatMap(flattenHierarchy).flatMap(_.datadumps)
      Future.sequence(uris.map(uri => downloader.download(uri)))
    }
    
    val f = for {
      voidFile <- downloader.download(voidURL)
      datasets <- parseVoID(voidFile)
      dumpfiles <- fHarvestDumpfiles(datasets)
    } yield (datasets, dumpfiles)
    
    f.map { case (datasets, dumpfiles) =>

      // TODO do something useful
      Logger.info("done")
      
      
    } recover { case t: Throwable =>
      Logger.error("Error harvesting dataset: " + t.getMessage)
      t.printStackTrace()
    }
  }
  
}