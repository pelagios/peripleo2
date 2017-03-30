package services.item.importers

import harvesting.HasBatchImport
import javax.inject.Inject
import services.item.{ ItemRecord, ItemService, ItemType }
import services.item.reference.UnboundReference
import services.task.TaskType

/** Just the generic item importer with no extra features **/
class ItemImporter @Inject() (
  itemService: ItemService,
  itemType: ItemType
) extends BaseImporter(itemService) with HasBatchImport[(ItemRecord, Seq[UnboundReference])] {
    
  override val TASK_TYPE = TaskType("ITEM_IMPORT")
  
  override val ITEM_TYPE = itemType 
  
}