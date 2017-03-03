package services.item

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

object ItemType extends Enumeration {
  
  /** An authority list.
    * Examples: a gazetteer, a person authority list.  
    */
  val AUTHORITY_LIST = Value("AUTHORITY_LIST")

  
  /** A dataset provided by an institution or indvidual.
    * Examples: a dataset of geospatial features, a numimatics
    * dataset, a corpus of literature.   
    */
  val DATASET = Value("DATASET")

  
  /** A geospatial feature.
    * Examples: outline of an archaeological trench, a
    * linestring of the course of a Roman road.
    */
  val FEATURE = Value("FEATURE")

  
  /** An object.
    * Examples: a coin, an inscription, a work of literature.
    */
  val OBJECT  = Value("OBJECT")
  
  
  /** A time period, as listed in one ore more period authority lists.
    * Example: "Aegean Bronze Age", as listed in PeriodO.
    */
  val PERIOD  = Value("PERIOD")
  
  
  /** A person, as listed in one or more person authority lists.
    * Example: Homer, as listed on VIAF.  
    */
  val PERSON  = Value("PERSON")
  
  /** A place, as listed in one ore more gazetteers.
    * Example: Athens, as listed in Pleiades, DARE and Vici.
    */
  val PLACE   = Value("PLACE")
  
  
  implicit val itemTypeFormat: Format[ItemType.Value] =
    Format(
      JsPath.read[JsString].map(json => ItemType.withName(json.value)),
      Writes[ItemType.Value](r => Json.toJson(r.toString))
    )

}