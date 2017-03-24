package services.item.search

import com.sksamuel.elastic4s.ElasticDsl._
import javax.inject.{ Inject, Singleton }
import org.elasticsearch.search.suggest.completion.CompletionSuggestion
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scala.collection.JavaConverters._
import scala.concurrent.{ ExecutionContext, Future }
import services.ES
import services.item.ItemType

case class Suggestion(text: String, 
  itemId: Option[String] = None,
  itemType: Option[ItemType.Value] = None, 
  description: Option[String] = None)
  
object Suggestion {
  
  implicit val suggestionWrites: Writes[Suggestion] = (
    (JsPath \ "text").write[String] and
    (JsPath \ "item_id").writeNullable[String] and
    (JsPath \ "item_type").writeNullable[ItemType.Value] and
    (JsPath \ "description").writeNullable[String]
  )(unlift(Suggestion.unapply))

}

@Singleton
class SuggestService @Inject() (val es: ES, implicit val ctx: ExecutionContext) {
  
  // TODO needs to be more complex once we get to entity-based
  // TODO suggestions. Probably need a SuggestResponse case class? 
  def suggest(query: String): Future[Seq[Suggestion]] =
    es.client execute {
      search in ES.PERIPLEO / ES.ITEM suggestions (
        phraseSuggestion("phrases") field("title") text(query) size 5,
        completionSuggestion("entities") field("suggest") text(query) size 5
      ) size 0
    } map { response =>
      val phrases = response.suggestion("phrases").entries
        .flatMap(option => option.optionsText.map(Suggestion(_)))

      // Completion response carry payload - need to go through Java API to get that      
      val entities = response.getSuggest.getSuggestion("entities").asInstanceOf[CompletionSuggestion]
        .getEntries.asScala
        .flatMap(_.getOptions.asScala)
        .map { option =>
          // val text = option.getText.string
          val payload = option.getPayloadAsMap
          Suggestion(
            option.getText.string,
            Option(payload.get("id")).map(_.toString),
            Option(payload.get("type")).map(t => ItemType.withName(t.toString)),
            Option(payload.get("description")).map(_.toString))
        }.toSeq
              
      phrases ++ entities
    }
      
}