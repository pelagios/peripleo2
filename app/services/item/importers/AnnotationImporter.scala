package services.item.importers

import javax.inject.{ Inject, Singleton }
import scala.concurrent.Future
import services.item.{ ItemRecord, ItemService, ItemType }
import services.item.reference.UnboundReference
import services.task.TaskType

@Singleton
class AnnotationImporter @Inject() (
  itemService: ItemService
) extends BaseItemImporter(itemService) with HasBatchImport[(ItemRecord, Seq[UnboundReference])] {
    
  override val TASK_TYPE = TaskType("ANNOTATION_IMPORT")
  
  def importDatasets(records: Seq[ItemRecord]): Future[Boolean] = {
    val padded = records.map(r => (r, Seq.empty[UnboundReference]))
    importBatch(padded, ItemType.DATASET).map { _.size == 0 }
  }
  
}