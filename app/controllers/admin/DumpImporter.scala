package controllers.admin

import java.io.{ File, FileInputStream, InputStream }
import java.util.zip.GZIPInputStream
import services.HasBatchImport
import services.task.TaskService

class DumpImporter(taskService: TaskService) {
  
  private def getStream(file: File, filename: String) =
    if (filename.endsWith(".gz"))
      new GZIPInputStream(new FileInputStream(file))
    else
      new FileInputStream(file)
  
  def importDump[T](file: File, filename: String, crosswalk: InputStream => Seq[T], service : HasBatchImport[T], username: String) = {
    val records = crosswalk(getStream(file, filename))
    
    // service.importBatch(records)
    
    // TODO progress feedback? Split in 10 batches and update progress after each?
    
  }
  
}