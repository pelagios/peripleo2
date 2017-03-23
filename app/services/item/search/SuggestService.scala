package services.item.search

import com.sksamuel.elastic4s.ElasticDsl._
import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }
import services.ES

@Singleton
class SuggestService @Inject() (val es: ES, implicit val ctx: ExecutionContext) {
  
  // TODO needs to be more complex once we get to entity-based
  // TODO suggestions. Probably need a SuggestResponse case class? 
  def suggest(query: String): Future[Seq[String]] =
    es.client execute {
      search in ES.PERIPLEO / ES.ITEM suggestions {
        completionSuggestion("autocomplete") field("autocomplete") text(query)
      } size 0
    } map { response =>
      
      play.api.Logger.info(response.toString)
      Seq.empty[String]
    }
      
}