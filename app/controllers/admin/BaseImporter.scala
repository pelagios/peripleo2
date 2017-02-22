package controllers.admin

import java.io.{ File, FileInputStream }
import java.util.zip.GZIPInputStream

class BaseImporter {
  
  protected def getStream(file: File, filename: String) =
    if (filename.endsWith(".gz")) new GZIPInputStream(new FileInputStream(file))
    else new FileInputStream(file)
  
}