package services.item

sealed trait Id

case object UndefinedId extends Id

case class DefinedId(value: String) extends Id

object Id {
  
  // For readability when creating Items/References
  val AUTOGENERATE = UndefinedId
  
  // TODO JSON (de)serialization
  
}
