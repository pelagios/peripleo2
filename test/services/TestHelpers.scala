package services

import java.io.File
import org.joda.time.{ DateTime, DateTimeZone } 
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.{ Json, Reads }
import scala.io.Source

trait TestHelpers {
  
  private val RESOURCES_PATH = "test/resources"
  
  private val DATETIME_PATTERN = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ")
  
  def loadJSON[T](relativePath: String)(implicit r: Reads[T]): T = {
    val path = new File(RESOURCES_PATH, relativePath)
    val json = Source.fromFile(path).getLines().mkString("\n")
    val result = Json.fromJson[T](Json.parse(json))
    
    if (result.isError)
      play.api.Logger.error(result.toString)
      
    result.get
  }
  
  def toDateTime(isoDate: String) = {
    DateTime.parse(isoDate, DATETIME_PATTERN).withZone(DateTimeZone.UTC)
  }
  
}