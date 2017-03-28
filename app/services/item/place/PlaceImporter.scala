package services.item.place

import java.util.UUID
import play.api.Logger
import scala.concurrent.Future
import services.HasBatchImport
import services.task.TaskType
import services.item.{ Item, ItemType, ItemRecord, TemporalBounds }

trait PlaceImporter extends HasBatchImport[ItemRecord] { self: PlaceService =>
  
  override val taskType = TaskType("GAZETTEER_IMPORT")
  
  private def MAX_URIS_IN_QUERY = 100 // Max URIs we will concatenate to an OR query
  
  private def getAffectedItems(normalizedRecord: ItemRecord): Future[Seq[Item]] = {
    // We need to query for this record's identifiers as well as all close/exactMatchURIs
    val identifiers = normalizedRecord.identifiers ++ normalizedRecord.allMatches

    // Protective measure - we don't really expect this to happen
    if (identifiers.size > MAX_URIS_IN_QUERY)
      throw new Exception("Maximum allowed number of close/exactMatch URIs exceeded by " + normalizedRecord.identifiers.head)

    findByPlaceOrMatchURIs(identifiers)
  }
  
  private def join(normalizedRecord: ItemRecord, items: Seq[Item]): Item = {
    // The general rule is that the "biggest item" (with highest number of records) determines
    // the docId and top-level properties of the conflated records
    val affectedItemsSorted = items.sortBy(- _.isConflationOf.size)
    val topItem = affectedItemsSorted.headOption
    val allRecords = items.flatMap(_.isConflationOf) :+ normalizedRecord
    
    val temporalBoundsUnion = allRecords.flatMap(_.temporalBounds) match {
      case bounds if bounds.size > 0 => Some(TemporalBounds.computeUnion(bounds))
      case _ => None
    }
    
    // Helper to get the first defined of a list of options
    def getFirst[T](a: Option[T], b: Option[T]) = Seq(a, b).flatten.headOption
    
    Item(
      topItem.map(_.docId).getOrElse(UUID.randomUUID),
      ItemType.PLACE,
      topItem.map(_.title).getOrElse(normalizedRecord.title),
      getFirst(topItem.flatMap(_.geometry), normalizedRecord.geometry),
      getFirst(topItem.flatMap(_.representativePoint), normalizedRecord.representativePoint),
      temporalBoundsUnion,
      allRecords
    )    
  }
  
  /** Conflates a list of M item records into N items (with N <= M) **/
  private def conflate(normalizedRecords: Seq[ItemRecord], items: Seq[Item] = Seq.empty[Item]): Seq[Item] = {

    // Conflates a single record
    def conflateOneRecord(r: ItemRecord, i: Seq[Item]): Seq[Item] = {
      val connectedItems= i.filter(_.isConflationOf.exists(_.isConnectedWith(r)))
      val unconnectedItems = items.diff(connectedItems)
      join(r, connectedItems) +: unconnectedItems
    }

    if (normalizedRecords.isEmpty) {
      items
    } else {
      val conflatedItems = conflateOneRecord(normalizedRecords.head, items)
      conflate(normalizedRecords.tail, conflatedItems)
    }
  }
  
  override def importRecord(record: ItemRecord): Future[Boolean] = {

    // Fetches affected places from the store and computes the new conflation
    def conflateAffectedItems(normalizedRecord: ItemRecord): Future[(Seq[Item], Seq[Item])] = {
      getAffectedItems(normalizedRecord).map(p => {
        // Sorted affected items by no. of records
        val affectedItems = p.sortBy(- _.isConflationOf.size)

        val affectedRecords = affectedItems
          .flatMap(_.isConflationOf) // all item records contained in the affected places
          .filter(_.uri != record.uri) // This record might update an existing record!

        val conflated = conflate(affectedRecords :+ normalizedRecord)

        // Pass back places before and after conflation
        (affectedItems, conflated)
      })
    }

    // Stores the newly conflated places to the store
    def storeUpdatedItems(itemsAfter: Seq[Item]): Future[Seq[Item]] =
      Future.sequence {
        itemsAfter.map(item => insertOrUpdatePlace(item).map((item, _)))
      } map { _.filter(!_._2).map(_._1) }

    // Deletes the places that no longer exist after the conflation from the store
    def deleteMergedPlaces(itemsBefore: Seq[Item], itemsAfter: Seq[Item]): Future[Seq[UUID]] =
      Future.sequence {
        // List of associations (Record URI -> Parent Item docId) before conflation
        val recordToParentMappingBefore = itemsBefore.flatMap(i =>
          i.isConflationOf.map(record => (record.uri, i.docId)))

        // List of associations (Record URI -> Parent Place RootURI) after conflation
        val recordToParentMappingAfter = itemsAfter.flatMap(i =>
          i.isConflationOf.map(record => (record.uri, i.docId)))

        // We need to delete all places that appear before, but not after the conflation
        val itemDocIdsBefore = recordToParentMappingBefore.map(_._2).distinct
        val itemDocIdsAfter = recordToParentMappingAfter.map(_._2).distinct

        val toDelete = itemDocIdsBefore diff itemDocIdsAfter
        toDelete.map(docId => deletePlace(docId).map(success => (docId, success)))
      } map { _.filter(!_._2).map(_._1) }

    for {
      (itemsBefore, itemsAfter) <- conflateAffectedItems(ItemRecord.normalize(record))
      failedUpdates <- storeUpdatedItems(itemsAfter)
      failedDeletes <- if (failedUpdates.isEmpty) deleteMergedPlaces(itemsBefore, itemsAfter) else Future.successful(Seq.empty[String])
    } yield failedUpdates.isEmpty && failedDeletes.isEmpty
  }
  
}