package services.item

sealed trait Id

case object UndefinedId extends Id

case class DefindedId(value: String) extends Id
