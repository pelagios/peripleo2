package services.item.importers

import harvesting.HasBatchImport
import javax.inject.Inject
import services.item.{ ItemRecord, ItemService, ItemType }
import services.item.reference.UnboundReference

/** Convenience wrapper around the BaseImporter, specifically for dataset record imports **/
class DatasetImporter @Inject() (
  itemService: ItemService
) extends BaseImporter(itemService) with HasBatchImport[ItemRecord] {
  
  override val ITEM_TYPE = ItemType.DATASET
  
  def importDatasets(records: Seq[ItemRecord]) =
    importBatch(records).map { _.size == 0 }
  
  def importRecord(record: ItemRecord) =
    super.importRecord((record, Seq.empty[UnboundReference]))
  
}