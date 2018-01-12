package controllers.admin.analytics

import controllers.{BaseAuthController, WebJarAssets}
import javax.inject.{Inject, Singleton}
import jp.t2v.lab.play2.auth.AuthElement
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.mvc.Action
import services.user.{Role, UserService}
import scala.concurrent.ExecutionContext
import services.visit.{VisitService, TimeInterval}
import services.profiling.ProfilingService

@Singleton
class AnalyticsAdminController @Inject() (
  val config: Configuration,
  val profiling: ProfilingService,
  val users:  UserService,
  val visits: VisitService,
  implicit val ctx: ExecutionContext,
  implicit val webjars: WebJarsUtil
) extends BaseAuthController with AuthElement {

  def index = AsyncStack(AuthorityKey -> Role.ADMIN) { implicit request =>
    val fProfile = profiling.getCollectionProfile()
    
    val fLast24Hrs =  visits.getStatsSince(TimeInterval.LAST_24HRS)
    val fLast7Days =  visits.getStatsSince(TimeInterval.LAST_7DAYS)
    val fLast30Days = visits.getStatsSince(TimeInterval.LAST_30DAYS)
    
    val f = for {
      profile    <- fProfile
      last24Hrs  <- fLast24Hrs
      last7Days  <- fLast7Days
      last30Days <- fLast30Days
    } yield (profile, last24Hrs, last7Days, last30Days)
    
    f.map { t =>
      Ok(views.html.admin.analytics.index(t._1, t._2, t._3, t._4))
    }
  }

}