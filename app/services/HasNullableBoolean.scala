package services

/** Convenience helper for serializing boolean fields.
  *
  * Instead of requiring 'false', the JSON can omit the field.
  */
trait HasNullableBoolean {
  
  protected def fromOptBool(o: Option[Boolean]) = o.getOrElse(false)

  protected def toOptBool(s: Boolean) = if (s) Some(true) else None
  
}
