package controllers.admin.analytics

import com.mohiva.play.silhouette.api.Silhouette
import controllers.{BaseAuthController, Security}
import javax.inject.{Inject, Singleton}
import org.webjars.play.WebJarsUtil
import play.api.Configuration
import play.api.mvc.ControllerComponents
import services.user.{Role, UserService}
import scala.concurrent.ExecutionContext
import services.visit.{VisitService, TimeInterval}
import services.profiling.ProfilingService

@Singleton
class AnalyticsAdminController @Inject() (
  val components: ControllerComponents,
  val config: Configuration,
  val profiling: ProfilingService,
  val users:  UserService,
  val visits: VisitService,
  val silhouette: Silhouette[Security.Env],
  implicit val ctx: ExecutionContext,
  implicit val webjars: WebJarsUtil
) extends BaseAuthController(components) {

  def index = silhouette.SecuredAction(Security.WithRole(Role.ADMIN)).async { implicit request =>
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