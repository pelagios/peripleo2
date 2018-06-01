package services.item.export

import services.item.{Item, TemporalBounds}
import com.vividsolutions.jts.io.WKTWriter
import com.vividsolutions.jts.geom.Geometry
import services.HasDate

case class ExportRecord(item: Item) {
  
  def tupled() = {
    val datasetId = item.isConflationOf.flatMap(_.isInDataset).flatMap(_.ids).headOption.getOrElse("")
    Seq(
      item.title,
      item.representativeGeometry.map(ExportRecord.toWKT(_)).getOrElse(""),
      item.temporalBounds.map(ExportRecord.toDateTime(_)).getOrElse(""),
      item.itemType.name,
      datasetId,
      item.isConflationOf.size.toString)
  }
  
}

object ExportRecord extends HasDate {
  
  private val wktWriter = new WKTWriter()
  
  def toWKT(geom: Geometry) = wktWriter.write(geom)
  
  def toDateTime(tBounds: TemporalBounds) = {    
    val duration = tBounds.to.minus(tBounds.from.getMillis)
    val  average = tBounds.from.plus(duration.getMillis)
    formatDate(average)    
  }
  
}


