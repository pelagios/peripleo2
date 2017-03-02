package services.item.place

import play.api.Logger
import scala.concurrent.Future
import services.HasBatchImport
import services.task.TaskType

trait PlaceImporter extends HasBatchImport[GazetteerRecord] { self: PlaceService =>
  
  override val taskType = TaskType("GAZETTEER_IMPORT")
  
  private def MAX_URIS_IN_QUERY = 100 // Max URIs we will concatenate to an OR query
  
  private def getAffectedPlaces(normalizedRecord: GazetteerRecord): Future[Seq[Place]] = {
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
  
  override def importRecord(record: GazetteerRecord): Future[Boolean] = {

    // Fetches affected places from the store and computes the new conflation
    def conflateAffectedPlaces(normalizedRecord: GazetteerRecord): Future[(Seq[Place], Seq[Place])] = {
      getAffectedPlaces(normalizedRecord).map(p => {
        // Sorted affected places by no. of gazetteer records
        val affectedPlaces = p.sortBy(- _.isConflationOf.size)

        val affectedRecords = affectedPlaces
          .flatMap(_.isConflationOf) // all gazetteer records contained in the affected places
          .filter(_.uri != record.uri) // This record might update an existing record!

        val conflated = conflate(affectedRecords :+ normalizedRecord)

        // Pass back places before and after conflation
        (affectedPlaces, conflated)
      })
    }

    // Stores the newly conflated places to the store
    def storeUpdatedPlaces(placesAfter: Seq[Place]): Future[Seq[Place]] =
      Future.sequence {
        placesAfter.map(place => insertOrUpdatePlace(place).map((place, _)))
      } map { _.filter(!_._2).map(_._1) }

    // Deletes the places that no longer exist after the conflation from the store
    def deleteMergedPlaces(placesBefore: Seq[Place], placesAfter: Seq[Place]): Future[Seq[String]] =
      Future.sequence {
        // List of associations (Record URI -> Parent Place RootURI) before conflation
        val recordToParentMappingBefore = placesBefore.flatMap(p =>
          p.isConflationOf.map(record => (record.uri, p.rootUri)))

        // List of associations (Record URI -> Parent Place RootURI) after conflation
        val recordToParentMappingAfter = placesAfter.flatMap(p =>
          p.isConflationOf.map(record => (record.uri, p.rootUri)))

        // We need to delete all places that appear before, but not after the conflation
        val placeRootURIsBefore = recordToParentMappingBefore.map(_._2).distinct
        val placeRootURIsAfter = recordToParentMappingAfter.map(_._2).distinct

        val toDelete = placeRootURIsBefore diff placeRootURIsAfter
        toDelete.map(rootURI => deletePlace(rootURI).map(success => (rootURI, success)))
      } map { _.filter(!_._2).map(_._1) }

    for {
      (placesBefore, placesAfter) <- conflateAffectedPlaces(GazetteerRecord.normalize(record))
      failedUpdates <- storeUpdatedPlaces(placesAfter)
      failedDeletes <- if (failedUpdates.isEmpty) deleteMergedPlaces(placesBefore, placesAfter) else Future.successful(Seq.empty[String])
    } yield failedUpdates.isEmpty && failedDeletes.isEmpty
  }
  
}