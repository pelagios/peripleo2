package services.item

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

sealed trait ItemType {
  
  val name: String
  
  val parent: Option[ItemType]
  
  lazy val asString: Seq[String] = parent match {
    case Some(parent) => parent.asString :+ name
    case None => Seq(name)
  }
  
}

/** Item type taxonomy **/
object ItemType {
  
  object DATASET extends ItemType { val name = "DATASET" ; val parent = None
   
    object AUTHORITY extends ItemType { val name = "AUTHORITY" ; val parent = Some(DATASET)
     
      object GAZETTEER extends ItemType { val name = "AUTHORITY_GAZETTEER" ; val parent = Some(AUTHORITY) }
      object PEOPLE    extends ItemType { val name = "AUTHORITY_PEOPLE"    ; val parent = Some(AUTHORITY) }
      object PERIODS   extends ItemType { val name = "AUTHORITY_PERIODS"   ; val parent = Some(AUTHORITY) }
     
    }
   
    object ANNOTATIONS extends ItemType { val name = "DATASET_ANNOTATIONS" ; val parent = Some(DATASET) }
    object GEODATA     extends ItemType { val name = "DATASET_GEODATA"     ; val parent = Some(DATASET) }
   
  }
 
  object OBJECT  extends ItemType { val name = "OBJECT"  ; val parent = None }
  object FEATURE extends ItemType { val name = "FEATURE" ; val parent = None }
  object PLACE   extends ItemType { val name = "PLACE"   ; val parent = None }
  object PERSON  extends ItemType { val name = "PERSON"  ; val parent = None }
  object PERIOD  extends ItemType { val name = "PERIOD"  ; val parent = None }
  
  // This is a bit of a nuisance, but can't find a better way
  private val LOOKUP_TABLE = 
    Seq(DATASET,
        DATASET.AUTHORITY,
        DATASET.AUTHORITY.GAZETTEER,
        DATASET.AUTHORITY.PEOPLE,
        DATASET.AUTHORITY.PERIODS,
        DATASET.ANNOTATIONS,
        DATASET.GEODATA,
        OBJECT,
        FEATURE,
        PLACE,
        PERSON,
        PERIOD).map(itemType => (itemType.asString.toSet -> itemType)).toMap
  
  def parse(s: Seq[String]): ItemType = LOOKUP_TABLE.get(s.toSet).get
 
  implicit val itemTypeFormat: Format[ItemType] = Format(
    JsPath.read[JsArray].map(json => parse(json.as[Seq[String]])),
    Writes[ItemType](t => Json.toJson(t.asString))
  )
 
}
