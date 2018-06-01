package services.item.export

import com.sksamuel.elastic4s.{Hit, HitReader}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.searches.RichSearchResponse
import es.ES
import java.nio.file.Paths
import javax.inject.{Inject, Singleton}
import kantan.csv.{CsvConfiguration, CsvWriter}
import kantan.csv.CsvConfiguration.{Header, QuotePolicy}
import kantan.csv.ops._
import play.api.libs.json.Json
import play.api.libs.Files.{TemporaryFile, TemporaryFileCreator}
import scala.concurrent.{ExecutionContext, Future}
import services.item.Item
import java.io.File

@Singleton
class DumpExporter @Inject() (
  implicit val ctx: ExecutionContext,
  implicit val tmpFile: TemporaryFileCreator
) {
  
  implicit object ItemHitReader extends HitReader[Item] {
    override def read(hit: Hit): Either[Throwable, Item] = {
      val item = Json.fromJson[Item](Json.parse(hit.sourceAsString)).get
      Right(item)
    }
  }
  
  private def fetchNextBatch(scrollId: String)(implicit es: ES) =
    es.client execute { searchScroll(scrollId) keepAlive "5m" }
  
  def scroll(
    fn: RichSearchResponse => Future[Boolean],
    response: RichSearchResponse,
    cursor: Long = 0l
  )(implicit es: ES, ctx: ExecutionContext): Future[Boolean] = 
    
    if (response.hits.isEmpty) {
      Future.successful(true)
    } else {
      fn(response).flatMap { success =>
        val processed = cursor + response.hits.size
        if (processed < response.totalHits)
          fetchNextBatch(response.scrollId).flatMap { response =>
            scroll(fn, response, processed).map(_ && success)
          }
        else
          Future.successful(success)
      }
    }
  
  def exportBatch(writer: CsvWriter[Seq[String]]): RichSearchResponse => Future[Boolean] = { response: RichSearchResponse =>
    Future {
      val records = response.to[Item].map(ExportRecord(_))
      records.foreach { rec => writer.write(rec.tupled) }
      true
    } recover { case t: Throwable =>
      t.printStackTrace
      throw t
    }
  }
  
  def exportAll()(implicit es: ES) = {
    val tmp = tmpFile.create(Paths.get(System.getProperty("java.io.tmpdir"), "all.csv"))
    val underlying = tmp.path.toFile
    
    val header = Seq("Title", "Geometry", "DateTime", "ItemType", "Dataset", "ConflatesNo")
    val config = CsvConfiguration(',', '"', QuotePolicy.WhenNeeded, Header.Explicit(header))  
    val writer = underlying.asCsvWriter[Seq[String]](config)

    es.client execute {
      search(ES.PERIPLEO/ ES.ITEM) query matchAllQuery limit 200 scroll "5m"
    } flatMap { scroll(exportBatch(writer), _) } map { success =>
      if (success) play.api.Logger.info("Export completed successfully. Yay!")
      else play.api.Logger.info("Export stopped. Something went wrong.")
      writer.close()
      success
    }
  }
  
}