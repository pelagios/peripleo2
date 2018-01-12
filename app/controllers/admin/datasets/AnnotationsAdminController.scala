package controllers.admin.datasets

import akka.actor.ActorSystem
import com.mohiva.play.silhouette.api.Silhouette
import controllers.{HasDatasetStats, Security}
import controllers.admin.authorities.BaseAuthorityAdminController
import harvesting.VoIDHarvester
import harvesting.loaders.DumpLoader
import harvesting.crosswalks._
import harvesting.crosswalks.tei.TeiCrosswalk
import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import org.webjars.play.WebJarsUtil
import play.api.{Configuration, Logger}
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.I18nSupport
import play.api.mvc.{ControllerComponents, MultipartFormData}
import play.api.mvc.MultipartFormData.FilePart
import play.api.libs.Files
import scala.concurrent.{ExecutionContext, Future}
import services.Sort
import services.item._
import services.item.search.SearchService
import services.item.importers.{DatasetImporter, ItemImporter, EntityImporter}
import services.task.{TaskService, TaskType}
import services.user.{Role, UserService}

@Singleton
class AnnotationsAdminController @Inject() (
  val components: ControllerComponents,
  val config: Configuration,
  val taskService: TaskService,
  val users: UserService,
  val voidHarvester: VoIDHarvester,
  val silhouette: Silhouette[Security.Env],
  implicit val itemService: ItemService,
  implicit val searchService: SearchService,
  implicit val ctx: ExecutionContext,
  implicit val system: ActorSystem,
  implicit val webjars: WebJarsUtil
) extends BaseAuthorityAdminController(components, new DatasetImporter(itemService, ItemType.DATASET.ANNOTATIONS))
  with HasDatasetStats
  with I18nSupport {

  private val PAGE_SIZE = 20

  def index(page: Option[Int]) = silhouette.SecuredAction(Security.WithRole(Role.ADMIN)).async { implicit request =>
    val offset = page.map(p => (Math.max(1, p) - 1) * PAGE_SIZE).getOrElse(0)

    itemService.findByType(
      ItemType.DATASET,
      true,
      offset,
      PAGE_SIZE,
      Some(Sort.ALPHABETICAL)
    ).flatMap { datasets =>
      addStats(datasets)
    } map { page => Ok(views.html.admin.datasets.annotations(page)) }
  }

  def importData = silhouette.SecuredAction(Security.WithRole(Role.ADMIN)) { implicit request =>
    val voidReq = request.body.asFormUrlEncoded
    val fileReq = request.body.asMultipartFormData

    def harvestVoID(form: Map[String, Seq[String]]) = form.get("url").flatMap(_.headOption) match {
      case Some(url) =>
        voidHarvester.harvest(url, request.identity.username).map { success =>
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
              request.identity.username)
          }}
          // TODO just a hack for now!!

        }

        Redirect(routes.AnnotationsAdminController.index(None))

      case None => BadRequest
    }

    (voidReq, fileReq) match {
      case (Some(form), _) => harvestVoID(form)
      case (_, Some(form)) => importFile(form)
      case _ => BadRequest
    }

  }

  def importIntoAuthority(f: FilePart[Files.TemporaryFile], itemType: ItemType, inDataset: PathHierarchy, username: String) = {
    val crosswalk = FeatureCollectionCrosswalk.fromGeoJSON(inDataset)
    val importer = new EntityImporter(itemService, itemType)
    new DumpLoader(taskService, TaskType("AUTHORITY_IMPORT")).importDump(
      "Importing authority data",
      f.ref.file,
      f.filename,
      crosswalk,
      importer,
      username)
  }

  def importIntoAnnotations(f: FilePart[Files.TemporaryFile], inDataset: PathHierarchy, username: String) = {
    val crosswalk =
      if (f.filename.endsWith(".tei.xml"))
        TeiCrosswalk.fromSingleFile(f.filename, inDataset)
      else
        PelagiosAnnotationCrosswalk.fromRDF(f.filename, inDataset)

    val importer = new ItemImporter(itemService, ItemType.OBJECT)
    new DumpLoader(taskService, TaskType("ANNOTATION_IMPORT")).importDump(
      "Importing into dataset",
      f.ref.file,
      f.filename,
      crosswalk,
      importer,
      username)
  }

  def importInto(datasetId: String) = silhouette.SecuredAction(Security.WithRole(Role.ADMIN)).async { implicit request =>
    request.body.asMultipartFormData match {
      case Some(form) => form.file("file") match {
        case Some(filepart) =>
          Logger.info("Importing into " + datasetId)

          // TODO check if valid file

          itemService.findByIdentifier(datasetId).map {
            case Some(dataset) =>
              // Safe to assume that datasets have exactly one record
              val record = dataset.isConflationOf.head
              val pathHierarchy =
                record.isPartOf.map(_.append(record.uri, record.title))
                  .getOrElse(PathHierarchy(record.uri, record.title))

              // Not ideal, but for the time being, we enforce a strict mapping
              // between dataset type and the types of items contained therein
              val itemType: ItemType = dataset.itemType match {
                case ItemType.DATASET.AUTHORITY.GAZETTEER => ItemType.PLACE
                case ItemType.DATASET.AUTHORITY.PEOPLE    => ItemType.PERSON
                case ItemType.DATASET.AUTHORITY.PERIODS   => ItemType.PERIOD
                case _                                    => ItemType.OBJECT
              }

              if (itemType == ItemType.OBJECT)
                importIntoAnnotations(filepart, pathHierarchy, request.identity.username)
              else
                importIntoAuthority(filepart, itemType, pathHierarchy, request.identity.username)

              Redirect(routes.AnnotationsAdminController.index(None))

            case None => NotFound
          }

        case None =>
          // Request without filepart? Bad request!
          Future.successful(BadRequest)
      }

      // Request without multipart form data? Bad request!
      case None => Future.successful(BadRequest)
    }
  }

  def deleteDataset(id: String) = silhouette.SecuredAction(Security.WithRole(Role.ADMIN)) { implicit request =>
    itemService.safeDeleteByDataset(id)
    Ok
  }

  val newDatasetForm = Form(
    mapping(
      "identifier" -> nonEmptyText,
      "item_type" -> nonEmptyText.transform[ItemType](ItemType.withName(_), _.toString),
      "title" -> nonEmptyText,
      "publisher" -> optional(text),
      "description" -> optional(text),
      "homepage" -> nonEmptyText,
      "license" -> optional(text)
    )(DatasetMeta.apply)(DatasetMeta.unapply)
  )

  def defineNewDataset = silhouette.SecuredAction(Security.WithRole(Role.ADMIN)) { implicit request =>
    Ok(views.html.admin.datasets.new_dataset(newDatasetForm))
  }

  def storeNewDataset = silhouette.SecuredAction(Security.WithRole(Role.ADMIN)).async { implicit request =>
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
            Seq.empty[Link],
            docMeta.publisher,
            None)

          importer.importRecord(record).map { success =>
            if (success) Redirect(controllers.admin.datasets.routes.AnnotationsAdminController.index(None))
            else InternalServerError
          }
        }
    )
  }

}

case class DatasetMeta(
  identifier  : String,
  itemType    : ItemType,
  title       : String,
  publisher   : Option[String],
  description : Option[String],
  homepage    : String,
  license     : Option[String])
