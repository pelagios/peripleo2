package services.item.place

import scala.concurrent.{ ExecutionContext, Future }
import services.HasBatchImport
import services.task.TaskType
import services.item.TemporalBounds

trait PlaceImporter extends HasBatchImport[GazetteerRecord] { self: PlaceService =>
  
  val taskType = TaskType("GAZETTEER_IMPORT")
  
  def importBatch(batch: Seq[GazetteerRecord]) = {
    
    play.api.Logger.info("Importing " + batch.size + " gazetteer records")
    
    // TODO implement
    Future.successful(Seq.empty[GazetteerRecord])
    
  }
  
}