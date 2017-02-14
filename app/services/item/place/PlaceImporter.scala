package services.item.place

import scala.concurrent.Future
import services.HasBatchImport
import services.task.TaskType

trait PlaceImporter extends HasBatchImport[GazetteerRecord] {
  
  val taskType = TaskType("GAZETTEER_IMPORT") 
  
  def importBatch(batch: Seq[GazetteerRecord]) = {
    
    play.api.Logger.info("Importing " + batch.size + " gazetteer records")
    
    // TODO implement
    Future.successful(Seq.empty[GazetteerRecord])
    
  }
  
}