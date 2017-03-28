package services.item

import scala.concurrent.Future
import services.HasBatchImport

trait ItemImporter extends HasBatchImport[ItemRecord] { self: ItemService =>

  /*
  private def MAX_URIS_IN_QUERY = 100 // Max URIs we will concatenate to an OR query
  
  private def getAffectedItems(normalizedRecord: ItemRecord): Future[Seq[Item]] = {
    // We need to query for this record's identifiers as well as all close/exactMatchURIs
    val identifiers = normalizedRecord.identifiers ++ normalizedRecord.allMatches

    // Protective measure - we don't really expect this to happen
    if (identifiers.size > MAX_URIS_IN_QUERY)
      throw new Exception("Maximum allowed number of close/exactMatch URIs exceeded by " + normalizedRecord.identifiers.head)

    findByPlaceOrMatchURIs(identifiers)
  }
  */
  
  
}