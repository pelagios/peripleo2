package services.item.search

import com.sksamuel.elastic4s.ElasticDsl._
import java.util.ArrayList
import javax.inject.{ Inject, Singleton }
import org.elasticsearch.common.unit.Fuzziness
import org.elasticsearch.search.suggest.completion.CompletionSuggestion
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.collection.JavaConverters._
import scala.concurrent.{ ExecutionContext, Future }
import es.ES
import services.item.ItemType

case class Suggestion(text: String, 
  itemId: Option[String] = None,
  itemType: Option[ItemType] = None, 
  description: Option[String] = None)
  
object Suggestion {
  
  implicit val suggestionWrites: Writes[Suggestion] = (
    (JsPath \ "text").write[String] and
    (JsPath \ "identifier").writeNullable[String] and
    (JsPath \ "item_type").writeNullable[ItemType] and
    (JsPath \ "description").writeNullable[String]
  )(unlift(Suggestion.unapply))

}

@Singleton
class SuggestService @Inject() (val es: ES, implicit val ctx: ExecutionContext) {
  
  def suggest(query: String): Future[Seq[Suggestion]] =
    es.client execute {
      search in ES.PERIPLEO / ES.ITEM suggestions (
        phraseSuggestion("from_titles").on("title") text(query) gramSize 3 size 3,
        phraseSuggestion("from_descriptions").on("is_conflation_of.descriptions.description") text(query)  gramSize 3 size 3,
        phraseSuggestion("from_context").on("quote.context") text(query)  gramSize 3 size 3,
        completionSuggestion("entities").on("suggest") fuzzyPrefixLength(3) text(query) size 5
      ) size 0
    } map { response =>
      
      play.api.Logger.info(response.toString)
      
      val phrases =
        Seq(
          response.suggestion("from_titles").entries,
          response.suggestion("from_descriptions").entries,
          response.suggestion("from_context").entries
        ).flatten
         .flatMap(suggestion => suggestion.options)
         .sortBy(- _.score)
         .map(option => Suggestion(option.text))
         .distinct
         .take(3)


      /* Completion response carry payload - need to go through Java API to get that      
      val entities = response.getSuggest.getSuggestion("entities").asInstanceOf[CompletionSuggestion]
        .getEntries.asScala
        .flatMap(_.getOptions.asScala)
        .map { option =>
          val payload = option.payload.getPayloadAsMap
          Suggestion(
            option.getText.string,
            Option(payload.get("identifier")).map(_.toString),
            Option(payload.get("type")).map(t => ItemType.parse(t.asInstanceOf[ArrayList[String]].asScala)),
            Option(payload.get("description")).map(_.toString))
        }.toSeq
        */
              
      phrases // ++ entities
    }
      
}