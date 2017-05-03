package services.item.importers

import harvesting.HasBatchImport
import javax.inject.Inject
import services.item.{ ItemRecord, ItemService, ItemType }
import services.item.reference.UnboundReference

/** Convenience wrapper around the BaseImporter, specifically for dataset record imports **/
class DatasetImporter(itemService: ItemService, itemType: ItemType) extends BaseImporter(itemService) with HasBatchImport[ItemRecord] {
  
  override val ITEM_TYPE = itemType
  
  // Datasets never carry references
  override val REJECT_IF_NO_REFERENCES = false
  
  def importDatasets(records: Seq[ItemRecord]) =
    importBatch(records).map { _.size == 0 }
  
  def importRecord(record: ItemRecord) =
    super.importRecord((record, Seq.empty[UnboundReference]))
  
}