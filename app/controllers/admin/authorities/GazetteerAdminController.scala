package controllers.admin.authorities

import akka.stream.Materializer
import controllers.{ BaseController, WebJarAssets }
import controllers.admin.StreamImporter
import java.io.FileInputStream
import javax.inject.{ Inject, Singleton }
import jp.t2v.lab.play2.auth.AuthElement
import play.api.{ Configuration, Logger }
import play.api.mvc.Action
import services.task.TaskService
import services.user.{ Role, UserService }

@Singleton
class GazetteerAdminController @Inject() (
  val config: Configuration,
  val users: UserService,
  val taskService: TaskService,
  val materializer: Materializer,
  implicit val webjars: WebJarAssets
) extends BaseController with AuthElement {

  def index = StackAction(AuthorityKey -> Role.ADMIN) { implicit request =>
    Ok(views.html.admin.authorities.gazetteers())
  }
  
  def importGazetteer = StackAction(AuthorityKey -> Role.ADMIN) { implicit request =>
    request.body.asMultipartFormData.flatMap(_.file("file")) match {
      case Some(formData) => {
        Logger.info("Importing gazetteer from " + formData.filename)
  
        /** TEMPORARY HACK **/
        if (formData.filename.contains(".ttl")) {
          Logger.info("Importing Pelagios RDF/TTL dump")
          // val importer = new DumpImporter()          
          // importer.importDump(formData.ref.file, formData.filename, PelagiosRDFCrosswalk.fromRDF(formData.filename))(places, ctx)
        } else if (formData.filename.toLowerCase.contains("pleiades")) {
          Logger.info("Using Pleiades crosswalk")
          val importer = new StreamImporter(taskService, materializer)
          // importer.importRecords(new FileInputStream(formData.ref.file), null, null, loggedIn.username)
          // importer.importPlaces(new FileInputStream(formData.ref.file), PleiadesCrosswalk.fromJson)(places, ctx)
        } else if (formData.filename.toLowerCase.contains("geonames")) {
          Logger.info("Using GeoNames crosswalk")
          // val importer = new StreamImporter()
          // importer.importPlaces(new FileInputStream(formData.ref.file), GeoNamesCrosswalk.fromJson)(places, ctx)
        }

        /** TEMPORARY HACK **/
        
        Redirect(routes.GazetteerAdminController.index)
      }
        
      case None => BadRequest
        
    }
  }

}