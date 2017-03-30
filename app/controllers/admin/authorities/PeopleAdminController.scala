package controllers.admin.authorities

import controllers.WebJarAssets
import javax.inject.{ Inject, Singleton }
import play.api.Configuration
import services.user.{ Role, UserService }
import services.item.ItemService
import services.item.importers.DatasetImporter

@Singleton
class PeopleAdminController @Inject() (
  val config: Configuration,
  val users: UserService,
  val itemService: ItemService,
  implicit val webjars: WebJarAssets
) extends BaseAuthorityAdminController(new DatasetImporter(itemService)) {

  def index = StackAction(AuthorityKey -> Role.ADMIN) { implicit request =>
    Ok(views.html.admin.authorities.people())
  }
  
  def importAuthorityFile = StackAction(AuthorityKey -> Role.ADMIN) { implicit request =>
    
    // TODO implement
    
    Ok
  }

}