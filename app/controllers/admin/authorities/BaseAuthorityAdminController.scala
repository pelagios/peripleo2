package controllers.admin.authorities

import org.joda.time.DateTime
import services.item._
import services.item.importers.DatasetImporter
import controllers.BaseAuthController
import play.api.mvc.ControllerComponents

abstract class BaseAuthorityAdminController(
  components: ControllerComponents,
  importer : DatasetImporter
) extends BaseAuthController(components) {
  
  protected def upsertDatasetRecord(
    uri           : String,
    title         : String,
    descriptions  : Seq[Description] = Seq.empty[Description],
    categories    : Seq[Category]    = Seq.empty[Category],
    license       : Option[String]   = None,
    logoUrl       : Option[String]   = None,
    lastChangedAt : Option[DateTime] = None
  ) = {
    
    val record = ItemRecord(
      uri,
      Seq(uri),      
      DateTime.now,
      lastChangedAt,
      title,
      None, None, // isInDataset, isPartOf
      categories,
      descriptions,
      None, // homepage
      license,
      Seq.empty[Language],
      logoUrl.map(url => Seq(Depiction(url, DepictionType.IMAGE))).getOrElse(Seq.empty[Depiction]),
      None, None, None, // geometry, representativePoint, temporalBounds
      Seq.empty[Name],
      Seq.empty[Link],
      None, None)
      
    importer.importRecord(record)
  }
  
}