package services

import com.sksamuel.elastic4s.{ ElasticClient, ElasticsearchClientUri }
import com.vividsolutions.jts.geom.{ Coordinate, GeometryFactory }
import java.io.File
import org.elasticsearch.common.settings.Settings
import org.joda.time.{ DateTime, DateTimeZone } 
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.{ Json, Reads }
import scala.io.Source
import java.util.UUID

trait TestHelpers {
  
  private val RESOURCES_PATH = "test/resources"
  
  private val DATETIME_PATTERN = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ")
  
  protected val CURRENT_TIME = DateTime.now().withZone(DateTimeZone.UTC).withMillisOfSecond(0)
  
  def loadTextfile(relativePath: String) = {
    val path = new File(RESOURCES_PATH, relativePath)
    Source.fromFile(path).getLines().mkString("\n")
  }
  
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
  
  def createPoint(lon: Double, lat: Double) =
    new GeometryFactory().createPoint(new Coordinate(14.02358, 48.31058))

  /*
  def createEmbeddedES(indexName: String): (File, ElasticClient) = {
    
    def initIndex(client: ElasticClient) = {
      val settings = Source.fromFile("conf/elasticsearch.json").getLines().mkString("\n")
      val create = client.admin.indices().prepareCreate(indexName)
      create.setSettings(settings)      
      create.execute().actionGet()
    }
    
    val indexDir = new File("test", "index-" + UUID.randomUUID)
    val remoteClient = ElasticClient.transport(ElasticsearchClientUri("localhost", 9300))

    val settings =
      Settings.settingsBuilder()
        .put("http.enabled", true)
        .put("path.home", indexDir.getAbsolutePath)
        .put ("script.inline", true)
        .put ("script.engine.groovy.inline.mapping", true)
        .put ("script.engine.groovy.inline.search", true)
        .put ("script.engine.groovy.inline.update", true)
        .put ("script.engine.groovy.inline.plugin", true)
        .put ("script.engine.groovy.inline.aggs", true)

    val client = ElasticClient.local(settings.build)
    
    Thread.sleep(1000) // Introduce wait time - local index init is slow that subsequent
    initIndex(client)
    Thread.sleep(2000)
    
    (indexDir, client)
  }
  */
  
}