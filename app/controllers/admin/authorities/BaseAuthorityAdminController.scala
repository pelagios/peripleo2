package controllers.admin.authorities

import org.joda.time.DateTime
import services.item._
import services.item.importers.DatasetImporter
import controllers.BaseAuthController
import jp.t2v.lab.play2.auth.AuthElement

abstract class BaseAuthorityAdminController(
  importer : DatasetImporter
) extends BaseAuthController with AuthElement {
  
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
      logoUrl.map(url => Seq(Depiction(url))).getOrElse(Seq.empty[Depiction]),
      None, None, None, // geometry, representativePoint, temporalBounds
      Seq.empty[Name],
      Seq.empty[String], // closeMatches
      Seq.empty[String]) // exactMatches
      
    importer.importRecord(record)
  }
  
}