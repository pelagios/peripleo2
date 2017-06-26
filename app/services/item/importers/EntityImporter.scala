package services.item.importers

import harvesting.HasBatchImport
import javax.inject.Inject
import services.item.{ ItemRecord, ItemService, ItemType }
import services.item.reference.{ ReferenceType, UnboundReference }

/** Convenience wrapper around the BaseImporter, specifically for entity imports **/
class EntityImporter(itemService: ItemService, itemType: ItemType) extends BaseImporter(itemService) with HasBatchImport[ItemRecord] {
  
  override val ITEM_TYPE = itemType
  
  // Entities always carry a single self-reference, so this wouldn't matter. However,
  // we'll set this flag to FALSE to account for the following case: 
  // in case of error and retry, the self-reference may already be in the index from 
  // the previous try. In this case, we want to index the item even if it has 0
  // indexable references.
  override val REJECT_IF_NO_INDEXABLE_REFERENCES = false 
  
  private val REFERENCE_TYPE = itemType match {
    case ItemType.PLACE  => ReferenceType.PLACE
    case ItemType.PERSON => ReferenceType.PERSON
    case ItemType.PERIOD => ReferenceType.PERIOD
    case _ =>
      throw new Exception("Not a valid entity item type: " + itemType)
  }
  
  override protected def importRecord(record: ItemRecord) =
    super.importRecord((record, Seq.empty[UnboundReference]))
  
}