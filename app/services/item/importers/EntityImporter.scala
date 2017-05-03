package services.item.importers

import harvesting.HasBatchImport
import javax.inject.Inject
import services.item.{ ItemRecord, ItemService, ItemType }
import services.item.reference.{ ReferenceType, UnboundReference }

/** Convenience wrapper around the BaseImporter, specifically for entity imports **/
class EntityImporter(itemService: ItemService, itemType: ItemType) extends BaseImporter(itemService) with HasBatchImport[ItemRecord] {
  
  override val ITEM_TYPE = itemType
  
  // Entities always carry a single self-reference, so 
  // should never be relevant, unless in case something breaks
  override val REJECT_IF_NO_REFERENCES = true 
  
  private val REFERENCE_TYPE = itemType match {
    case ItemType.PLACE  => ReferenceType.PLACE
    case ItemType.PERSON => ReferenceType.PERSON
    case ItemType.PERIOD => ReferenceType.PERIOD
    case _ =>
      throw new Exception("Not a valid entity item type: " + itemType)
  }
  
  override protected def importRecord(record: ItemRecord) = {
    // Due to the way indexing works, entities contain a single reference to themselves
    val ref = UnboundReference(
      record.uri,
      REFERENCE_TYPE,
      record.uri, None, None, None, None)
      
    super.importRecord((record, Seq(ref)))
  }
  
}