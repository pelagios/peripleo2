package es

import com.google.inject.AbstractModule
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{ ElasticClient, ElasticsearchClientUri }
import java.io.File
import javax.inject.{ Inject, Singleton }
import org.elasticsearch.common.settings.Settings
import play.api.{ Configuration, Logger }
import play.api.inject.ApplicationLifecycle
import scala.io.Source
import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._
import scala.util.{ Try, Success, Failure }
import scala.concurrent.Await

/** Binding ES as eager singleton, so we can start & stop properly **/
class ESModule extends AbstractModule {

  def configure = {
    bind(classOf[ES]).asEagerSingleton
  }

}

/** Constants **/
object ES extends ESQuerySanitizer {

  // Index name
  val PERIPLEO = "peripleo"

  // Mapping type names
  val ITEM      = "item"
  val REFERENCE = "reference"
  val TASK      = "task"
  val USER      = "user"
  val VISIT     = "visit"

  // Maximum response size in ES
  val MAX_SIZE           = 10000

  // Max. number of retries to do in case of failure
  val MAX_RETRIES        = 10

}

/** ElasticSearch client + start & stop helpers **/
@Singleton
class ES @Inject() (
    config: Configuration, lifecycle: ApplicationLifecycle
) extends HasDeleteByQuery {

  start()

  lifecycle.addStopHook { () => Future.successful(stop()) }

  lazy val client = {
    val home = config.getString("peripleo.index.dir") match {
      case Some(dir) => new File(dir)
      case None => new File("index")
    }

    val remoteClient = ElasticClient.transport(ElasticsearchClientUri("localhost", 9300))

    // Just fetch cluster stats to see if there's a cluster at all
    Try(
      Await.result(remoteClient execute {
        get cluster stats
      }, 3.seconds)
    ) match {
      case Success(_) => {
        // Yes, there's a cluster! Let's use that one, shall we?
        Logger.info("Joining ElasticSearch cluster")
        remoteClient
      }

      case Failure(_) => {
        // No ES cluster available? I'll have my own local node!
        val settings =
          Settings.settingsBuilder()
            .put("http.enabled", true)
            .put("path.home", home.getAbsolutePath)

        Logger.info("Local index - using " + home.getAbsolutePath + " as location")
        val client = ElasticClient.local(settings.build)

        // Introduce wait time, otherwise local index init is so slow that subsequent
        // .isExists request returns false despite an existing index (note: this is only
        // relevant in dev mode, anyway)
        Thread.sleep(1000)
        client
      }
    }
  }
  
  def refreshIndex()(implicit ctx: ExecutionContext): Future[Boolean] = 
    client execute {
      refresh index ES.PERIPLEO
    } map { _ => true } recover { case t: Throwable => false }

  private def start() = {
    implicit val timeout = 60.seconds
    val response = client.execute { index exists(ES.PERIPLEO) }.await

    if (response.isExists()) {
      // Index exists - create missing mappings if needed
      val list = client.admin.indices().prepareGetMappings()
      val existingMappings = list.execute().actionGet().getMappings().get(ES.PERIPLEO).keys.toArray.map(_.toString)
      loadMappings(existingMappings).foreach { case (name, json) =>
        Logger.info("Creating mapping " + name)
        val putMapping = client.admin.indices().preparePutMapping(ES.PERIPLEO)
        putMapping.setType(name)
        putMapping.setSource(json)
        putMapping.execute().actionGet()
      }
    } else {
      // Peripleo index doesn't exist - create
      Logger.info("No ES index - initializing...")

      val create = client.admin.indices().prepareCreate(ES.PERIPLEO)
      create.setSettings(loadSettings())

      loadMappings().foreach { case (name, json) =>  {
        Logger.info("Create mapping - " + name)
        create.addMapping(name, json)
      }}

      create.execute().actionGet()
    }
  }

  private def stop() = {
    Logger.info("Stopping ElasticSearch local node")
    client.close()
  }

  private def loadSettings(): String =
    Source.fromFile("conf/elasticsearch.json").getLines().mkString("\n")

  /** Loads JSON mapping files from the /conf directory, optionally excepting specific mappings **/
  private def loadMappings(except: Seq[String] = Seq.empty[String]): Seq[(String, String)] =
    new File("conf/es-mappings").listFiles.toSeq.filter(_.getName.endsWith(".json"))
      .foldLeft(Seq.empty[(Int, (String, String))])((mappings, file)  => {
        val number = file.getName.substring(0, 2).toInt
        val name = file.getName.substring(3, file.getName.lastIndexOf('.'))
        if (except.contains(name)) {
          mappings
        } else {
          val json = Source.fromFile(file).getLines.mkString("\n")
          mappings :+ (number, (name, json))
        }
      }).sortBy(_._1).map(_._2)

}
