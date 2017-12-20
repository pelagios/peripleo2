package services.item.importers

import com.sksamuel.elastic4s.ElasticDsl._
import es.ES
import java.util.UUID
import play.api.Logger
import scala.concurrent.Future
import scala.language.postfixOps
import services.item._
import services.item.reference.{ Reference, UnboundReference }
import services.task.TaskType
import services.item.ItemService.ItemWithUnboundReferences

abstract class BaseImporter(itemService: ItemService) {

  /** The ItemType this importer should set for new items **/
  protected val ITEM_TYPE: ItemType

  /**
   *  If true, the importer will reject items that have 0 resolvable references.
   *  This is normally desired behavior. (E.g. we don't want place references
   *  in the system we can't map.) But not in all cases. (E.g. we want to allow
   *  dataset items, even though the dataset items don't carry references
   *  themselves.
   */
  protected val REJECT_IF_NO_INDEXABLE_REFERENCES: Boolean

  val es = itemService.es

  implicit val ctx = itemService.ctx

  import ItemService._

  private def MAX_URIS_IN_QUERY = 100 // Max URIs we will concatenate to an OR query

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
    }.map(_.toReference(i.item))

    // Filter references, keeping only those that are not already indexed
    def indexableReferences(selfReferences: Seq[Reference], resolvableReferences: Seq[Reference]) = {

      // If a reference is i) a self-reference AND ii) resolvable, it's already indexed!
      val dontIndex = selfReferences.distinct intersect resolvableReferences.distinct

      // If we had an item merge, resolvable self-references may point to the old doc_id. Rebind.
      val rebound = resolvableReferences.map { reference =>
        if (reference.parentUri == reference.referenceTo.uri)
          reference.rebind(i.item)
        else
          reference
      }

      // All refs, minus those indexed already - that's what we need to index
      val all = (selfReferences ++ rebound).distinct
      all diff dontIndex
    }

    def fUpsert(item: Item, maybeVersion: Option[Long], refs: Seq[Reference]) =
      if (REJECT_IF_NO_INDEXABLE_REFERENCES && refs.size == 0) {
        Logger.warn("Skipping item with 0 resolvable references")
        i.references.foreach(ref => Logger.warn(ref.toString))
        Future.successful(true)
      } else {
        val upsertItem = maybeVersion match {
          case Some(version) => update id item.docId.toString in ES.PERIPLEO / ES.ITEM source item version version
          case None => index into ES.PERIPLEO / ES.ITEM id item.docId.toString source item
        }

        if (refs.size > 20)
          Logger.warn("Inserting " + refs.size + " references to index")
          
        // https://stackoverflow.com/questions/35758990/out-of-memory-in-elasticsearch
        es.client.client.prepareBulk()
        
        es.client execute {
          // Failures will occasionally happen here due version conflicts (ES optimistic locking!).
          // Note that in this case, the bulk insert will produce orphaned References! We could
          // separate insert of Item and References, and only insert the References in case the item insert
          // was successful. But we'd sacrifice some performance because of two insert requests instead of
          // one. Therefore, we accept the orphaned References at this point, and clean them up later
          // in the Reference-rewrite stage (cf. ReferenceService).
          bulk ( upsertItem +: refs.map(ref => index into ES.PERIPLEO / ES.REFERENCE source ref parent item.docId.toString) )
        } map { result =>
          // Note: it seems we cannot reliably roll back Reference inserts following a version conflict. Immediately
          // after insert, they are not necessarily indexed, so a delete request will just fail.
          if (result.hasFailures) {
            result.failures.foreach(result => Logger.error(result.failureMessage))
            Logger.error("Error indexing item: " + i.item.docId)
          }
          !result.hasFailures
        }
      }

    val f = for {
      resolvedReferences <- fFilterResolvable
      success <- fUpsert(i.item, i.maybeVersion, indexableReferences(selfReferences, resolvedReferences))
    } yield success

    f.recover { case t: Throwable =>
      Logger.error("Error indexing item " + i.item.docId + ": " + t.getMessage)
      t.printStackTrace
      false
    }
  }

  /** Fetches all items from the index that will be affected from adding this record **/
  private def getAffectedItems(normalizedRecord: ItemRecord): Future[Seq[ItemWithReferences]] = {
    // We need to query for this record's identifiers as well as all close/exactMatchURIs
    val identifiers = normalizedRecord.identifiers ++ normalizedRecord.directMatches

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
    val startTime = System.currentTimeMillis

    // Helper to attach references to their matching parent items **/
    def groupReferences(items: Seq[(Item, Option[Long])], unbound: Seq[UnboundReference]): Seq[ItemWithUnboundReferences] =
      items.map { case (item, version) =>
        val uris = item.identifiers.toSet
        val references = unbound.filter(ref => uris.contains(ref.parentUri))
        ItemWithUnboundReferences(item, version, references)
      }

    // Fetches affected items from the index and computes the new conflation
    def reconflateItems(normalizedRecord: ItemRecord, references: Seq[UnboundReference]): Future[(Seq[ItemWithReferences], Seq[ItemWithUnboundReferences])] = {

      getAffectedItems(normalizedRecord).map { i =>

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

        val conflatedItems: Seq[(Item, Option[Long])] = {

          val itemsAfterConflation = conflate(records)

          if (affectedItems.size > 0 && affectedItems.size != itemsAfterConflation.size)
            Logger.info("Re-conflating " + affectedItems.size + " items to " + itemsAfterConflation.size + " items")

          // Optimization: in case multiple items are merged into one,
          // retain docId, so we need to rewrite fewer references
          if (affectedItems.size > 0 && itemsAfterConflation.size == 1) {
            val oneBefore = affectedItems.head
            val after = itemsAfterConflation.head
            Seq((after.copy(docId = oneBefore.item.docId), Some(oneBefore.version)))
          } else {
            itemsAfterConflation.map((_, None))
          }
        }

        val conflatedReferences = affectedItems.flatMap(_.references).map(_.unbind) ++ references

        // Pass back items before and after conflation
        (affectedItems, groupReferences(conflatedItems, conflatedReferences))
      }
    }

    // Deletes items that will be merged into other items from the index
    def deleteMergedItems(itemsBefore: Seq[ItemWithReferences], itemsAfter: Seq[ItemWithUnboundReferences]): Future[Seq[UUID]] = {
      // val startTime = System.currentTimeMillis
      
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
      
      Future.sequence {
        toDelete.map(docId => itemService.deleteById(docId).map(success => (docId, success)))
      } map { failed =>
        // Logger.debug("Deleting merged items took " + (System.currentTimeMillis - startTime))
        failed.filter(!_._2).map(_._1)
      }
    }

    // Stores the newly conflated items in the index
    def storeUpdatedItems(itemsAfter: Seq[ItemWithUnboundReferences]): Future[Seq[ItemWithUnboundReferences]] =
      Future.sequence {
        itemsAfter.map(i => insertOrUpdateItem(i).map((i, _)))
      } map { failed =>
        failed.filter(!_._2).map(_._1)
      }

    val f = for {
      (itemsBefore, itemsAfter) <- reconflateItems(ItemRecord.normalize(record), references.map(_.normalize))
      failedDeletes <- deleteMergedItems(itemsBefore, itemsAfter)
      if (failedDeletes.isEmpty)
      failedUpdates <- storeUpdatedItems(itemsAfter)
      if (failedUpdates.isEmpty)
      referencesRewritten <- itemService.rewriteReferencesTo(itemsBefore.map(_.item), itemsAfter.map(_.item))
    } yield referencesRewritten

    f.map { success =>
      val took = System.currentTimeMillis - startTime
      if (took > 1000) {
        Logger.info(s"Indexing ${tuple._1.uri} took ${took} ms")
        
        // Give ElasticSearch a break, or it will... break 
        Thread.sleep(5000) 
      }
      success 
    }.recover { case t: Throwable =>
      t.printStackTrace()
      false
    }
  }

}
