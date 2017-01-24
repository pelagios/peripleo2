package controllers.admin

import javax.inject.{ Inject, Singleton }
import play.api.mvc.{ Action, Controller }

@Singleton
class GazetteerAdminController @Inject() extends Controller {

  def index = Action {
    Ok(views.html.admin.gazetteers())
  }

}
