package harvesting

import java.io.File
import java.net.URL
import play.api.Logger
import play.api.libs.Files.TemporaryFile
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.postfixOps
import sys.process._

class FileDownloader(url: String, filename: String) {
  
  private val TMP_DIR = System.getProperty("java.io.tmpdir")
  
  private val MAX_RETRIES = 5
  
  // TODO if we want to compute file hashes (to check for changes) - this is probably the place to put the code
  
  def download(retries: Int = MAX_RETRIES)(implicit ctx: ExecutionContext): Future[TemporaryFile] = Future { 
    scala.concurrent.blocking {
      Logger.info("Starting download from " + url)
      
      // TODO don't hand filename into class, but use UUID name instead?
      val tempFile = new TemporaryFile(new File(TMP_DIR, filename))   
      
      new URL(url) #> tempFile.file !!
      
      Logger.info("Download complete for " + url)
      tempFile
    }
  } recoverWith { case t: Throwable =>
    if (retries > 0) download(retries - 1)
    else throw t
  }
  
}