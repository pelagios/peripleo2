package services.item.importers

import harvesting.HasBatchImport
import javax.inject.Inject
import services.item.{ ItemRecord, ItemService, ItemType }
import services.item.reference.UnboundReference

/** Convenience wrapper around the BaseImporter, specifically for entity imports **/
class EntityImporter(itemService: ItemService, itemType: ItemType) extends BaseImporter(itemService) with HasBatchImport[ItemRecord] {
  
  override val ITEM_TYPE = itemType
  
  // Entities don't usually carry a reference
  override val REJECT_IF_NO_INDEXABLE_REFERENCES = false 
  
  override protected def importRecord(record: ItemRecord) =
    super.importRecord((record, Seq.empty[UnboundReference]))
  
}