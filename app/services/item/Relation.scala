package services.item

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

object Relation extends Enumeration {

  /** The reference provides evidence for the existence of the item.
    * Examples: reference to a place on an inscription, reference to
    * a person on a coin. 
    * 
    * SCOPE: valid on OBJECT items only.
    * RANGE: valid on PERSON, PLACE and FEATURE references. 
    */
  val ATTESTATION = Value("ATTESTATION")
  
  
  /** The reference is to the creator of the item.
    * Examples: the Iliad(=Item) referencing Homer(=Person).
    * 
    * SCOPE: valid on DATASET, FEATURE and OBJECT items.
    * RANGE: valid on PERSON references only.
    */
  val CREATOR = Value("CREATOR")
  
  
  /** The reference is to the findspot of the item.
    * Example: findspot of an archaeological object.
    *
    * SCOPE: valid on OBJECT items only.
    * RANGE: valid for PLACE and FEATURE references.
    */
  val FINDSPOT = Value("FINDSPOT")

  
  /** The reference is to a (current or previous) location of the object.
    * Example: an object held in a museum.
    * 
    * SCOPE: valid on OBJECT items only.
    * RANGE: valid for PLACE and FEATURE references.
    */
  val LOCATION = Value("LOCATION")
  
  
  /** The referenced place is a waypoint in the feature.
    * Example: a place through which an ancient road (=Feature) crosses through.  
    * 
    * SCOPE: valid on FEATURE items only.
    * RANGE: valid for PLACE and FEATURE references.
    */
  val WAYPOINT = Value("WAYPOINT")
    
  
  implicit val relationFormat: Format[Relation.Value] =
    Format(
      JsPath.read[JsString].map(json => Relation.withName(json.value)),
      Writes[Relation.Value](r => Json.toJson(r.toString))
    )

}