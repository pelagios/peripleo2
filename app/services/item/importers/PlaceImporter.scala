package services.item.importers

import harvesting.HasBatchImport
import javax.inject.Inject
import services.item.{ ItemRecord, ItemService, ItemType }
import services.item.reference.{ ReferenceType, UnboundReference }
import services.task.TaskType

class PlaceImporter @Inject() (
  itemService: ItemService
) extends BaseItemImporter(itemService) with HasBatchImport[ItemRecord] {
  
  override val TASK_TYPE = TaskType("GAZETTEER_IMPORT")
  
  override protected def importRecord(record: ItemRecord, itemType: ItemType) = {
    val ref = UnboundReference(
      record.uri,
      ReferenceType.PLACE,
      record.uri, None, None, None, None)

    super.importRecord((record, Seq(ref)), ItemType.PLACE)
  }
  
}