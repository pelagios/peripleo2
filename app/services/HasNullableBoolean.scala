package services

trait HasNullableBoolean {
  
  protected def fromOptBool(o: Option[Boolean]) =
    o.getOrElse(false) // If the boolean field is not there, it's false by convention

  protected def toOptBool(s: Boolean) =
    if (s) Some(true) else None
  
}
