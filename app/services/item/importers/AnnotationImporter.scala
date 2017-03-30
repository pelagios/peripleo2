package services.item.importers

import harvesting.HasBatchImport
import javax.inject.Inject
import services.item.{ ItemRecord, ItemService, ItemType }
import services.item.reference.UnboundReference
import services.task.TaskType

class AnnotationImporter @Inject() (
  itemService: ItemService
) extends BaseItemImporter(itemService) with HasBatchImport[(ItemRecord, Seq[UnboundReference])] {
    
  override val TASK_TYPE = TaskType("ANNOTATION_IMPORT")
  
  def importDatasets(records: Seq[ItemRecord]) = {
    val padded = records.map(r => (r, Seq.empty[UnboundReference]))
    importBatch(padded, ItemType.DATASET).map { _.size == 0 }
  }
  
}