package controllers.admin.datasets

import akka.actor.ActorSystem
import controllers.{ WebJarAssets, HasDatasetStats }
import controllers.admin.authorities.BaseAuthorityAdminController
import harvesting.VoIDHarvester
import harvesting.loaders.DumpLoader
import harvesting.crosswalks._
import harvesting.crosswalks.tei.TeiCrosswalk
import javax.inject.{ Inject, Singleton }
import play.api.{ Configuration, Logger }
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.mvc.MultipartFormData
import play.api.libs.Files
import scala.concurrent.{ ExecutionContext, Future }
import services.item._
import services.item.importers.DatasetImporter
import services.item.search.SearchService
import services.task.{ TaskService, TaskType }
import services.user.{ Role, UserService }
import services.item.ItemService
import services.item.importers.ItemImporter
import org.joda.time.DateTime

@Singleton
class AnnotationsAdminController @Inject() (
  val config: Configuration,
  val taskService: TaskService,
  val users: UserService,
  val voidHarvester: VoIDHarvester,
  val messagesApi: MessagesApi,
  implicit val itemService: ItemService,
  implicit val searchService: SearchService,
  implicit val ctx: ExecutionContext,
  implicit val system: ActorSystem,
  implicit val webjars: WebJarAssets
) extends BaseAuthorityAdminController(new DatasetImporter(itemService, ItemType.DATASET.ANNOTATIONS))
  with HasDatasetStats
  with I18nSupport {

  def index = AsyncStack(AuthorityKey -> Role.ADMIN) { implicit request =>
    itemService.findByType(ItemType.DATASET).flatMap { datasets =>
      addStats(datasets)
    } map { page => Ok(views.html.admin.datasets.annotations(page)) }
  }

  def importData = StackAction(AuthorityKey -> Role.ADMIN) { implicit request =>
    val voidReq = request.body.asFormUrlEncoded
    val fileReq = request.body.asMultipartFormData

    def harvestVoID(form: Map[String, Seq[String]]) = form.get("url").flatMap(_.headOption) match {
      case Some(url) =>
        voidHarvester.harvest(url, loggedIn.username).map { success =>
          if (success) Logger.info(s"Import complete from $url")
          else Logger.error(s"Import failed from $url")
        } recover { case t: Throwable =>
          t.printStackTrace
          Logger.info("Error harvesting VoID: " + t.getMessage)
        }
        Ok // Return immediately

      case _ => BadRequest
    }

    def importFile(form: MultipartFormData[Files.TemporaryFile]) = form.file("file") match {
      case Some(filepart) =>
        if (filepart.filename.contains(".tei.xml")) {
          Logger.info("Importing TEI")

          // TODO just a hack for now!!
          upsertDatasetRecord("TEI", "TEI").map { success => if (success) {
            val importer = new ItemImporter(itemService, ItemType.OBJECT)
            new DumpLoader(taskService, TaskType("TEI_IMPORT")).importDump(
              filepart.filename,
              filepart.ref.file,
              filepart.filename,
              TeiCrosswalk.fromSingleFile(filepart.filename, PathHierarchy("TEI", "TEI")),
              importer,
              loggedIn.username)
          }}
          // TODO just a hack for now!!

        }

        Redirect(routes.AnnotationsAdminController.index)

      case None => BadRequest
    }

    (voidReq, fileReq) match {
      case (Some(form), _) => harvestVoID(form)
      case (_, Some(form)) => importFile(form)
      case _ => BadRequest
    }

  }

  def deleteDataset(id: String) = StackAction(AuthorityKey -> Role.ADMIN) { implicit request =>
    itemService.safeDeleteByDataset(id)
    Ok
  }

  val newDatasetForm = Form(
    mapping(
      "identifier" -> nonEmptyText,
      "item_type" -> nonEmptyText.transform[ItemType](ItemType.withName(_), _.toString),
      "title" -> nonEmptyText,
      "description" -> optional(text),
      "homepage" -> nonEmptyText,
      "license" -> optional(text)
    )(DatasetMeta.apply)(DatasetMeta.unapply)
  )

  def defineNewDataset = StackAction(AuthorityKey -> Role.ADMIN) { implicit request =>
    Ok(views.html.admin.datasets.new_dataset(newDatasetForm))
  }

  def storeNewDataset = AsyncStack(AuthorityKey -> Role.ADMIN) { implicit request =>
    newDatasetForm.bindFromRequest.fold(
        formWithErrors =>
          Future.successful(BadRequest(views.html.admin.datasets.new_dataset(formWithErrors))),

        docMeta => {
          val importer = new DatasetImporter(itemService, docMeta.itemType)
          val record = ItemRecord(
            docMeta.identifier,
            Seq(docMeta.identifier),
            DateTime.now,
            None, // lastChangedAt
            docMeta.title,
            None, None, // isInDataset, isPartOf
            Seq.empty[Category],
            docMeta.description.map(d => Seq(Description(d))).getOrElse(Seq.empty[Description]),
            Some(docMeta.homepage),
            docMeta.license,
            Seq.empty[Language],
            Seq.empty[Depiction],
            None, None, None, // geometry, representativePoint, temporalBounds
            Seq.empty[Name],
            Seq.empty[String], // closeMatches
            Seq.empty[String]) // exactMatches

          importer.importRecord(record).map { success =>
            if (success) Redirect(controllers.admin.datasets.routes.AnnotationsAdminController.index())
            else InternalServerError
          }
        }
    )
  }

}

case class DatasetMeta(identifier: String, itemType: ItemType, title: String, description: Option[String], homepage: String, license: Option[String])
