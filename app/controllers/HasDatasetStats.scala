package controllers

import scala.concurrent.{ExecutionContext, Future}
import services.Page
import services.item.{Item, ItemService}
import services.item.search._
import services.item.search.filters.TermFilter

trait HasDatasetStats {
  
  private def getItemCount(datasets: Seq[Item])(implicit ctx: ExecutionContext, searchService: SearchService): Future[Seq[Long]] = {
    val ids = datasets.map(_.isConflationOf.head.uri)
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
        byDataset.find(_._1.startsWith(d.isConflationOf.head.uri)).map(_._2).getOrElse(0l)
      }
    }
  }
    
  private def getSubsetCount(datasets: Seq[Item])(implicit ctx: ExecutionContext, itemService: ItemService): Future[Seq[Long]] =
    Future.sequence(
      datasets.map(d => itemService.findByIsPartOf(d.isConflationOf.head, true, 0, 0).map(_.total))
    )
  
  def addStats(datasets: Page[Item])(implicit ctx: ExecutionContext, itemService: ItemService, searchService: SearchService): Future[Page[(Item, Long, Int)]] = {
    val fItemCount = getItemCount(datasets.items)
    val fSubsetCount = getSubsetCount(datasets.items)
    
    val f = for {
      itemCount <- fItemCount
      subsetCount <- fSubsetCount
    } yield (itemCount, subsetCount)
    
    f.map { case (itemCount, subsetCount) => 
      datasets.zip(itemCount).zip(subsetCount).map { case ((dataset, items), subsets) =>
        (dataset, items, subsets.toInt)        
      }
    }    
  }
  
}