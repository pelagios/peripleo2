package controllers.admin.analytics

import controllers.{ BaseAuthController, WebJarAssets }
import javax.inject.{ Inject, Singleton }
import jp.t2v.lab.play2.auth.AuthElement
import play.api.Configuration
import play.api.mvc.Action
import services.user.{ Role, UserService }
import services.visit.{ VisitService, TimeInterval }

@Singleton
class AnalyticsAdminController @Inject() (
  val config: Configuration,
  val users:  UserService,
  val visits: VisitService,
  implicit val webjars: WebJarAssets
) extends BaseAuthController with AuthElement {

  def index = StackAction(AuthorityKey -> Role.ADMIN) { implicit request =>
    visits.getStatsSince(TimeInterval.LAST_24HRS)
    visits.getStatsSince(TimeInterval.LAST_7DAYS)
    Ok(views.html.admin.analytics.index())
  }

}