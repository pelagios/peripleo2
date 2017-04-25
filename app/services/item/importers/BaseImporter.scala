package services.item.importers

import com.sksamuel.elastic4s.ElasticDsl._
import java.util.UUID
import play.api.Logger
import scala.concurrent.Future
import scala.language.postfixOps
import services.{ ES }
import services.item._
import services.item.reference.{ Reference, UnboundReference }
import services.task.TaskType
import services.item.ItemService.ItemWithUnboundReferences

abstract class BaseImporter(itemService: ItemService) {
  
  protected val ITEM_TYPE: ItemType
  
  val es = itemService.es
  
  implicit val ctx = itemService.ctx
  
  import ItemService._
  
  private def MAX_URIS_IN_QUERY = 100 // Max URIs we will concatenate to an OR query
  
  def insertOrUpdateItem(item: Item, references: Seq[UnboundReference]): Future[Boolean] =
    insertOrUpdateItem(ItemWithUnboundReferences(item, references))
  
  /** The bare-bones code for upserting an item and its references
    *
    * TODO at the moment, this works for items - but not for references!
    *   
    */
  private def insertOrUpdateItem(i: ItemWithUnboundReferences): Future[Boolean] = {
    // References that resolve to objects already in the index
    val fFilterResolvable = itemService.resolveReferences(i.references)
    
    // References that resolve to the item itself (in case of places, persons, periods)
    val selfReferences = i.references.filter { ref =>
      val identifiers = i.item.identifiers.toSet
      identifiers.contains(ref.uri)
    }.map(_.toReference(i.item.docId))
        
    // Existing references become unbound during the conflation stage, so we just
    // batch-delete all associated references (and re-insert below)
    val fDeleteExistingReferences = 
      es.deleteByQuery(ES.REFERENCE, termQuery("reference_to.doc_id", i.item.docId.toString), Some(i.item.docId.toString))
            
    def fUpsert(item: Item, refs: Seq[Reference]) = {
      es.client execute {
        bulk (
          // Upsert the item
          { update id item.docId.toString in ES.PERIPLEO / ES.ITEM source item docAsUpsert } +: refs.map { ref => 
            
            // Insert the references
            index into ES.PERIPLEO / ES.REFERENCE source ref parent item.docId.toString }
        )
      }
    }
      
    val f = for {
      resolvedReferences <- fFilterResolvable
      _ <- fDeleteExistingReferences
      _ <- fUpsert(i.item, resolvedReferences ++ selfReferences)
    } yield true
    
    f.recover { case t: Throwable =>
      Logger.error("Error indexing item " + i.item.docId + ": " + t.getMessage)
      t.printStackTrace
      false
    }
  }

  /** Fetches all items from the index that will be affected from adding this record **/
  private def getAffectedItems(normalizedRecord: ItemRecord): Future[Seq[ItemWithReferences]] = {
    // We need to query for this record's identifiers as well as all close/exactMatchURIs
    val identifiers = normalizedRecord.identifiers ++ normalizedRecord.allMatches

    // Protective measure - we don't really expect this to happen
    if (identifiers.size > MAX_URIS_IN_QUERY)
      throw new Exception("Maximum allowed number of close/exactMatch URIs exceeded by " + normalizedRecord.identifiers.head)

    itemService.findConnected(identifiers)
  }
  
  /** Joins a record with a list of items **/
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
    
    Item.fromRecords(topItem.map(_.docId).getOrElse(UUID.randomUUID), ITEM_TYPE, allRecords)
  }
  
  /** Conflates a list of M records into N items (with N <= M), depending on how they are connected **/
  private def conflate(normalizedRecords: Seq[ItemRecord], items: Seq[Item] = Seq.empty[Item]): Seq[Item] = {

    // Conflates a single record
    def conflateOneRecord(r: ItemRecord, i: Seq[Item]): Seq[Item] = {
      val connectedItems = i.filter(_.isConflationOf.exists(_.isConnectedWith(r)))
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

  /** Imports a single item record and connected references **/
  protected def importRecord(tuple: (ItemRecord, Seq[UnboundReference])): Future[Boolean] = tuple match { case (record, references) =>
    
    // Helper to attach references to their matching parent items **/
    def groupReferences(items: Seq[Item], unbound: Seq[UnboundReference]): Seq[ItemWithUnboundReferences] =
      items.map { item =>
        val uris = item.isConflationOf.flatMap(_.identifiers).toSet
        val references = unbound.filter(ref => uris.contains(ref.parentUri))
        ItemWithUnboundReferences(item, references)
      }

    // Fetches affected items from the index and computes the new conflation
    def reconflateItems(normalizedRecord: ItemRecord, references: Seq[UnboundReference]): Future[(Seq[ItemWithReferences], Seq[ItemWithUnboundReferences])] = {
      // val startTime = System.currentTimeMillis
      getAffectedItems(normalizedRecord).map(i => {
        // Sorted affected items by no. of records
        val affectedItems = i.sortBy(- _.item.isConflationOf.size)

        val records = {
          // All records
          val affectedRecords = affectedItems.flatMap(_.item.isConflationOf)
          
          // This record might update an existing one - find out where it is in the list
          val replaceIdx = affectedRecords.indexWhere(_.uri == normalizedRecord.uri)
          if (replaceIdx < 0)
            affectedRecords :+ normalizedRecord
          else
            affectedRecords.patch(replaceIdx, Seq(normalizedRecord), 1)
        }
        
        val conflatedItems = {
          val itemsAfterConflation = conflate(records)
          
          // Optimization: in case we had one item before, and one item after conflation, 
          // then conflation did nothing but add or update an additional record. In this case,
          // retain the docId, so we don't need to rewrite references
          if (affectedItems.size == 1 && itemsAfterConflation.size == 1) {
            val before = affectedItems.head.item
            val after = itemsAfterConflation.head
            Seq(after.copy(docId = before.docId))
          } else {
            if (affectedItems.size > 0) {
              Logger.info("Re-conflating " + affectedItems.size + " items to " + itemsAfterConflation.size + " items")
              Logger.info("  before: " + affectedItems.map(_.item.docId).mkString(", "))
              Logger.info("  after:  " + itemsAfterConflation.map(_.docId).mkString(", "))
            }
            itemsAfterConflation
          }
        }
        
        val conflatedReferences = 
          affectedItems.flatMap(_.references).map(_.unbind) ++ references
        
        // Logger.debug("Fetch & reconflation took " + (System.currentTimeMillis - startTime))

        // Pass back places before and after conflation
        (affectedItems, groupReferences(conflatedItems, conflatedReferences))
      })
      
    }
    
    // Deletes items that will be merged into other items from the index
    def deleteMergedItems(itemsBefore: Seq[ItemWithReferences], itemsAfter: Seq[ItemWithUnboundReferences]): Future[Seq[UUID]] = {
      // val startTime = System.currentTimeMillis
      Future.sequence {
        // List of associations (Record URI -> Parent Item docId) before conflation
        val recordToParentMappingBefore = itemsBefore.flatMap(i =>
          i.item.isConflationOf.map(record => (record.uri, i.item.docId)))

        // List of associations (Record URI -> Parent Item docId) after conflation
        val recordToParentMappingAfter = itemsAfter.flatMap(i =>
          i.item.isConflationOf.map(record => (record.uri, i.item.docId)))

        // We need to delete all items that appear before, but not after the conflation
        val docIdsBefore = recordToParentMappingBefore.map(_._2).distinct
        val docIdsAfter = recordToParentMappingAfter.map(_._2).distinct

        val toDelete = docIdsBefore diff docIdsAfter
        toDelete.map(docId => itemService.deleteById(docId).map(success => (docId, success)))
      } map { failed =>
        // Logger.debug("Deleting merged items took " + (System.currentTimeMillis - startTime))
        failed.filter(!_._2).map(_._1) 
      }
    }

    // Stores the newly conflated items in the index
    def storeUpdatedItems(itemsAfter: Seq[ItemWithUnboundReferences]): Future[Seq[ItemWithUnboundReferences]] = {
      // val startTime = System.currentTimeMillis
      Future.sequence {
        itemsAfter.map(i => insertOrUpdateItem(i).map((i, _)))
      } map { failed =>
        // Logger.debug("Storing updated items took " + (System.currentTimeMillis - startTime))
        failed.filter(!_._2).map(_._1) 
      }
    }
    
    val f = for {
      (itemsBefore, itemsAfter) <- reconflateItems(ItemRecord.normalize(record), references.map(_.normalize))
      failedUpdates <- storeUpdatedItems(itemsAfter)
      if (failedUpdates.isEmpty)
      referencesRewritten <- itemService.rewriteReferencesTo(itemsBefore.map(_.item), itemsAfter.map(_.item)) 
      if (referencesRewritten)
      failedDeletes <- deleteMergedItems(itemsBefore, itemsAfter)
    } yield failedDeletes.isEmpty
    
    f.recover { case t: Throwable =>
      t.printStackTrace()
      false
    }
  }  
  
}