package harvesting

import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import java.io.{ File, FileOutputStream }
import java.nio.file.Files
import java.util.UUID
import play.api.Logger
import play.api.libs.ws.WSClient
import play.api.libs.Files.TemporaryFile
import scala.concurrent.{ ExecutionContext, Future }

trait HasFileDownload { self: { val ws: WSClient } =>
  
  private val TMP_DIR = System.getProperty("java.io.tmpdir")
  
  private val MAX_RETRIES = 5
  
  private val KNOWN_EXTENSIONS = Set("rdf", "rdf.xml", "ttl", "n3", "json")
  
  private val EXTENSION_BY_CONTENT_TYPE = Seq(
    "application/rdf+xml" -> "rdf",
    "text/xml" -> "rdf",
    "text/turtle" -> "ttl",
    "text/n3" -> "n3",
    "application/json" -> "json")
  
  // TODO if we want to compute file hashes (to check for changes) - this is probably the place to put the code
 
  private class UnknownFormatError(url: String, msg: String) extends Exception(msg)
    
  private def getExtension(url: String, contentType: Option[String]): String =
    KNOWN_EXTENSIONS.find(ext => url.endsWith(ext)) match {
      case Some(extension) =>
        // If the URL ends with a known extension - fine
        extension
        
      case None if contentType.isDefined =>
        // If not, try via response Content-Type
        EXTENSION_BY_CONTENT_TYPE.find { case (prefix, extension) =>
          contentType.get.startsWith(prefix)
        } match {
          case Some((_, extension)) => extension
          case None => 
            Logger.error(s"Could not determine content type for $url (" + contentType.get + ")")
            throw new UnknownFormatError(url, "Unsupported file format: " + contentType.get)
        }
        
      case _ =>
        Logger.error(s"Could not determine content type for $url (no content type)")
        throw new UnknownFormatError(url, "Unknown file format (no content type)")
    }
  
  protected def download(url: String, retries: Int = MAX_RETRIES)(implicit ctx: ExecutionContext, mat: Materializer) : Future[TemporaryFile] = {
    Logger.info("Downloading from " + url)
    val filename = UUID.randomUUID.toString
    val tempFile = new TemporaryFile(new File(TMP_DIR, filename + ".download"))
    val fStream = ws.url(url).withFollowRedirects(true).stream()
    
    fStream.flatMap { response =>
      val outputStream = new FileOutputStream(tempFile.file)      
      val sink = Sink.foreach[ByteString](bytes => outputStream.write(bytes.toArray))
      
      response.body.runWith(sink).andThen {
        case result =>
          outputStream.close()
          result.get
      } map {_ =>
        Logger.info("Download complete")
        val contentType = response.headers.headers.get("Content-Type").flatMap(_.headOption)
        val extension = getExtension(url, contentType)
        val renamedTempFile = new TemporaryFile(new File(TMP_DIR, filename + "." + extension))
        Files.copy(tempFile.file.toPath, renamedTempFile.file.toPath) 
        tempFile.finalize()
        renamedTempFile
      }
    } recoverWith {
      case t: UnknownFormatError =>
        // Break immediately
        throw t
      
      case t: Throwable =>  
        // Retry
        if (retries > 0) download(url, retries - 1)
        else throw t
    }
  }

}
