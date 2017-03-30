package harvesting.loaders

import java.io.{ File, FileInputStream }
import java.util.zip.GZIPInputStream

trait BaseLoader {
  
  protected def getStream(file: File, filename: String) =
    if (filename.endsWith(".gz")) new GZIPInputStream(new FileInputStream(file))
    else new FileInputStream(file)
  
}