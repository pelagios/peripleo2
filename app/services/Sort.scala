package services

import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.ElasticDsl._
import scala.language.implicitConversions

object Sort extends Enumeration {
  
  val ALPHABETICAL = Value("ALPHABETICAL")
  
  private[services] implicit def asES(v: Sort.Value) = v match {
    case ALPHABETICAL => field sort "title.raw" 
  }
  
}