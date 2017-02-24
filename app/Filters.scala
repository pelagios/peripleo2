import javax.inject._

import akka.stream.Materializer
import play.api._
import play.api.http.HttpFilters
import play.api.mvc._
import play.filters.gzip.GzipFilter

/**
 * This class configures filters that run on every request. This
 * class is queried by Play to get a list of filters.
 *
 * Play will automatically use filters from any class called
 * `Filters` that is placed the root package. You can load filters
 * from a different class by adding a `play.http.filters` setting to
 * the `application.conf` configuration file.
 *
 * @param env Basic environment settings for the current application.
 * @param exampleFilter A demonstration filter that adds a header to
 * each response.
 */
@Singleton
class Filters @Inject() (implicit materializer: Materializer) extends HttpFilters {
  
  private val gzipFilter = new GzipFilter(shouldGzip = (request, response) =>
    response.body.contentType.exists { contentType => 
      contentType.startsWith("application/json")
    })

  override val filters = Seq(gzipFilter)

}
