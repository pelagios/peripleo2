package services.item.search.filters

import com.sksamuel.elastic4s.HitReader
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.searches.queries.QueryDefinition
import es.ES
import java.util.UUID
import org.apache.lucene.search.join.ScoreMode
import scala.concurrent.{ ExecutionContext, Future }
import services.item.search.RichResultItem

case class ReferencedItemFilter(uris: Seq[String], setting: TermFilter.Setting) {
  
 def filterDefinition()(implicit es: ES, ctx: ExecutionContext): Future[QueryDefinition] = {
    
    // In a first step, we need to get from URIs to DocIDs
    def resolve: Future[Seq[UUID]] = es.client execute {
      search(ES.PERIPLEO / ES.ITEM) query {
        boolQuery should (uris.map(termQuery("is_conflation_of.uri", _)))
      }
    } map { _.hits.map(hit => UUID.fromString(hit.id)).toSeq }
    
    def filter(docId: UUID) = 
      hasChildQuery(ES.REFERENCE) query { termQuery("reference_to.doc_id", docId.toString) }
   
    if (uris.size == 1)
      // One item URI - just use a single filter clause
      resolve.map(docIds => filter(docIds.head) scoreMode ScoreMode.Avg)   
    else
      // Multiple URIs - multiple filter clauses, AND'ed together
      // TODO support OR later
      resolve.map(docIds => must { docIds.map(filter(_) scoreMode ScoreMode.Avg) })
  }
  
}