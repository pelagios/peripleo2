package services.profiling

import com.sksamuel.elastic4s.ElasticDsl._
import javax.inject.{Inject, Singleton}
import es.ES
import scala.concurrent.ExecutionContext

@Singleton
class ProfilingService @Inject() (val es: ES, implicit val ctx: ExecutionContext) {
  
  def getCollectionProfile() =
    es.client execute {
      search(ES.PERIPLEO / ES.ITEM) query {
        constantScoreQuery {
          termQuery("is_conflation_of.depictions.depiction_type" -> "IIIF")
        }
      } size 0
    } map { response =>
      CollectionProfile(response.totalHits)      
    }

}