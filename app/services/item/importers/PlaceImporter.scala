package services.item.importers

import javax.inject.{ Inject, Singleton }
import scala.concurrent.Future
import services.item.{ ItemRecord, ItemService, ItemType }
import services.item.reference._
import services.task.TaskType

@Singleton
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