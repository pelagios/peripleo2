package services.item.search

import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.ElasticDsl._
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import services.HasDate

case class TimerangeResponse(from: DateTime, to: DateTime, totalHits: Long)

trait HasTimerangeQuery extends HasDate { self: SearchService =>
  
  def getTimerange(args: SearchArgs) =
    SearchFilter.build(args).flatMap { filter =>
      val q = self.itemBaseQuery(args, filter.withoutDateRangeFilter)
      es.client execute { 
        q limit 0 start 0 aggs (
          minAggregation("from") field "temporal_bounds.from",
          maxAggregation("to")   field "temporal_bounds.to" 
        )
      } map { response =>
        val from = 
          response.aggregations.minResult("from").getValueAsString
        val to =
          response.aggregations.maxResult("to").getValueAsString
             
        val p = ISODateTimeFormat.dateParser()
        TimerangeResponse(p.parseDateTime(from), p.parseDateTime(to), response.totalHits) 
      }
    }
  
}
