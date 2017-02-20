package services.item.place

import play.api.Logger
import scala.concurrent.{ ExecutionContext, Future }
import services.HasBatchImport
import services.task.TaskType
import services.item.TemporalBounds

trait PlaceImporter extends HasBatchImport[GazetteerRecord] { self: PlaceService =>
  
  val taskType = TaskType("GAZETTEER_IMPORT")
  
  private def MAX_URIS_IN_QUERY = 100 // Max URIs we will concatenate to an OR query
  
  private def MAX_RETRIES = 5 // Max times an update will be retried in case of failure
  
  private def getAffectedPlaces(normalizedRecord: GazetteerRecord)(implicit context: ExecutionContext): Future[Seq[Place]] = {
    // We need to query for this record's URI as well as all close/exactMatchURIs
    val uris = normalizedRecord.uri +: normalizedRecord.allMatches

    // Protective measure - we don't really expect this to happen
    if (uris.size > MAX_URIS_IN_QUERY)
      throw new Exception("Maximum allowed number of close/exactMatch URIs exceeded by " + normalizedRecord.uri)

    findByPlaceOrMatchURIs(uris)
  }
  
  private def join(normalizedRecord: GazetteerRecord, places: Seq[Place]): Place = {
    // The general rule is that the "biggest place" (with highest number of gazetteer records) determines
    // ID and title of the conflated places
    val affectedPlacesSorted = places.sortBy(- _.isConflationOf.size)
    val rootUri = affectedPlacesSorted.headOption.map(_.rootUri)
    val allRecords = places.flatMap(_.isConflationOf) :+ normalizedRecord
    
    Place(rootUri.getOrElse(normalizedRecord.uri), allRecords)
  }
  
  /** Conflates a list of M gazetteer records into N places (with N <= M) **/
  private def conflate(normalizedRecords: Seq[GazetteerRecord], places: Seq[Place] = Seq.empty[Place]): Seq[Place] = {

    // Conflates a single record
    def conflateOneRecord(r: GazetteerRecord, p: Seq[Place]): Seq[Place] = {
      val connectedPlaces = p.filter(_.isConflationOf.exists(_.isConnectedWith(r)))
      val unconnectedPlaces = places.diff(connectedPlaces)
      join(r, connectedPlaces) +: unconnectedPlaces
    }

    if (normalizedRecords.isEmpty) {
      places
    } else {
      val conflatedPlaces = conflateOneRecord(normalizedRecords.head, places)
      conflate(normalizedRecords.tail, conflatedPlaces)
    }
  }
  
  private def importRecord(record: GazetteerRecord): Future[Boolean] = ???
  
  private def importRecords(records: Seq[GazetteerRecord], retries: Int = MAX_RETRIES): Future[Seq[GazetteerRecord]] =
    records.foldLeft(Future.successful(Seq.empty[GazetteerRecord])) { case (f, record) =>
      f.flatMap { failed =>
        importRecord(record).map { success =>
          if (success) failed
          else record +: failed 
        }
      }
    } flatMap { failedRecords =>
      Logger.info("Imported " + (records.size - failedRecords.size) + " records") 
      if (failedRecords.size > 0 && retries > 0) {
        Logger.warn(failedRecords.size + " gazetteer records failed to import - retrying")
        importRecords(failedRecords, retries - 1)
      } else {
        if (failedRecords.size > 0) Logger.error(failedRecords.size + " gazetteer records failed without recovery")
        else Logger.info("No failed imports")
        Future.successful(failedRecords)
      }
    }
  
  override def importBatch(batch: Seq[GazetteerRecord]) = importRecords(batch)
  
}