package controllers

import scala.concurrent.{ ExecutionContext, Future }
import services.Page
import services.item.Item
import services.item.search._
import services.item.search.filters.TermFilter

trait HasDatasetStats {
  
  def addStats(datasets: Page[Item])(implicit ctx: ExecutionContext, searchService: SearchService): Future[Page[(Item, Long)]] = {
    val ids = datasets.items.map(_.isConflationOf.head.uri)
    val filters = SearchFilters(
      None, // item type
      None, // category
      Some(TermFilter(ids, TermFilter.ONLY)), // 
      None, // languages
      None, // referenced item
      None, // date range
      None, // spatial filer
      None, // has depiction
      true) // root only
    
    val settings = ResponseSettings(false, true, false)
  
    val searchArgs = SearchArgs(
      None, // query
      0, 0,
      filters,
      settings)
      
    searchService.query(searchArgs).map { result =>     
      val byDataset = result.aggregations.find(_.name == "by_dataset").get.buckets
      
      datasets.map { d =>
        val count = byDataset.find(_._1.startsWith(d.isConflationOf.head.uri)).map(_._2).getOrElse(0l)
        (d, count)
      }
    }    
  }
  
}