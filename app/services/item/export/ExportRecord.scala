package services.item.export

import com.vividsolutions.jts.io.WKTWriter
import com.vividsolutions.jts.geom.{Envelope, Geometry, GeometryFactory}
import org.joda.time.format.DateTimeFormat
import scala.concurrent.{ExecutionContext, Future}
import services.item.{Item, ItemService,TemporalBounds}
import es.ES

case class ExportRecord(item: Item) {
  
  import ExportRecord._
  
  def tupled()(implicit itemService: ItemService, ctx: ExecutionContext): Future[Seq[String]] = {
    val datasetId = item.isConflationOf.flatMap(_.isInDataset).flatMap(_.ids).headOption.getOrElse("")
    
    val fGeometry: Future[Option[Geometry]] = item.representativeGeometry match {
      case Some(geom) =>
        Future.successful(Some(geom))
        
      case None =>
        // No geometry - retrieve references and build from those
        itemService.getReferences(item.identifiers.head, None, None, 0, ES.MAX_SIZE).map { results =>
          if (results.items.size > 0) {            
            val refs = results.items.map(_._1)
            val bboxes = refs.flatMap(_.referenceTo.bbox)
            Some(mergeBounds(bboxes))
          } else {
            None
          }
        }
        
    }
    
    fGeometry.map { geom =>
      Seq(
        item.title,
        geom.map(toWKT(_)).getOrElse(""),
        item.temporalBounds.map(toDateTime(_)).getOrElse(""),
        item.itemType.name,
        datasetId,
        item.isConflationOf.size.toString)
    }
  }
  
}

object ExportRecord {
  
  private val dateFormatter = DateTimeFormat.forPattern("yyyy/MM/dd HH:mm")
  
  private val wktWriter = new WKTWriter()
  
  private val factory = new GeometryFactory()
  
  def mergeBounds(bounds: Seq[Envelope]): Geometry = {
    if (bounds.size == 1) {
      factory.toGeometry(bounds.head)
    } else {
      val geoms = bounds.map(factory.toGeometry(_))
      factory.createGeometryCollection(geoms.toArray)
    }
  }
  
  def toWKT(geom: Geometry) = wktWriter.write(geom)
  
  def toDateTime(tBounds: TemporalBounds) = {    
    val duration = tBounds.to.minus(tBounds.from.getMillis)
    val average = tBounds.from.plus(duration.getMillis)
    dateFormatter.print(average)    
  }
  
}


