package es

import com.sksamuel.elastic4s.{ QueryDefinition, RichSearchResponse, RichSearchHit }
import com.sksamuel.elastic4s.ElasticDsl._
import java.util.UUID
import play.api.Logger
import scala.concurrent.{ ExecutionContext, Future }

trait HasDeleteByQuery { self: ES =>
  
  def fetchNextBatch(scrollId: String): Future[RichSearchResponse] =
    client execute {
      search scroll scrollId keepAlive "1m"
    }
  
  def deleteOneBatch(ids: Seq[String], index: String, parent: Option[String])(implicit ctx: ExecutionContext): Future[Boolean] =
    client execute {
      bulk (
        parent match {
          case Some(parentId) =>
            ids.map { id => delete id id from ES.PERIPLEO / index parent parentId }

          case None =>
            ids.map { id => delete id id from ES.PERIPLEO / index }
        }
      )
    } map { result =>
      if (result.hasFailures)
        Logger.error("Error deleting by query: " + result.failures.map(_.failureMessage).mkString("\n"))

      !result.hasFailures
    }
    
  
  /** A generic helper for deleting records by query **/
  private def queryDelete(index: String, q: QueryDefinition, parent: Option[String])(implicit ctx: ExecutionContext): Future[Boolean] = {

    def deleteBatch(response: RichSearchResponse, cursor: Long = 0l): Future[Boolean] = {
      val ids = response.hits.map(_.id)
      val total = response.totalHits
  
      if (ids.isEmpty)
        Future.successful(true)
      else
        deleteOneBatch(ids, index, parent).flatMap { success =>
          val deletedRecords = cursor + ids.size
          if (deletedRecords < total)
            fetchNextBatch(response.scrollId).flatMap(deleteBatch(_, deletedRecords).map(_ && success))
          else
            Future.successful(success)
        }
    }

    client execute {
      search in ES.PERIPLEO / index query q limit 50 scroll "1m"
    } flatMap {
      deleteBatch(_)
    }
  }
  
  private def conditionalQueryDelete(index: String, q: QueryDefinition, condition: RichSearchHit => Future[Boolean])(implicit ctx: ExecutionContext): Future[Boolean] = {
    
    def deleteBatch(response: RichSearchResponse, cursor: Long = 0l): Future[Boolean] = {
      val total = response.totalHits
      if (response.hits.isEmpty) {
        Future.successful(true)
      } else {
        val verified = Future.sequence(
          response.hits.toSeq.map { hit => condition(hit).map((hit.id, _)) })
        
        verified.flatMap { result =>
          val idsToDelete = result.filter(_._2).map(_._1)
          deleteOneBatch(idsToDelete, index, None).flatMap { success =>
            val processedRecords = cursor + response.hits.size
            if (processedRecords < total)
              fetchNextBatch(response.scrollId).flatMap(deleteBatch(_, processedRecords).map(_ && success))
            else
              Future.successful(success)
          }
        }
      }
    }
    
    client execute {
      search in ES.PERIPLEO / index query q limit 20 scroll "1m"
    } flatMap {
      deleteBatch(_)
    }
  }
  
  def deleteChildrenByQuery(index: String, q: QueryDefinition)(implicit ctx: ExecutionContext): Future[Boolean] = {
    
    def deleteOneByOne(hits: Seq[RichSearchHit]): Future[Boolean] =
      hits.foldLeft(Future.successful(true)) { case (fSuccess, hit) =>
        fSuccess.flatMap { success =>
          client execute {
            delete id hit.id from ES.PERIPLEO / index parent hit.field("_parent").value.toString
          } map { _ => success
          } recover { case t: Throwable => false }
        }
      } 
    
    def deleteBatch(response: RichSearchResponse, cursor: Long = 0l): Future[Boolean] = {
      if (response.hits.isEmpty) {
        Future.successful(true)
      } else {
        deleteOneByOne(response.hits).flatMap { success =>
          val deletedRecords = cursor + response.hits.size
          if (deletedRecords < response.totalHits)
            fetchNextBatch(response.scrollId).flatMap(deleteBatch(_, deletedRecords).map(_ && success))
          else
            Future.successful(success)
        }
      }
    }
    
    client execute {
      search in ES.PERIPLEO / index query q limit 50 scroll "1m"
    } flatMap {
      deleteBatch(_)
    }    
  }
  
  def deleteByQuery(index: String, q: QueryDefinition)(implicit ctx:ExecutionContext): Future[Boolean] =
    queryDelete(index, q, None)

  def deleteChildrenByParent(index: String, parentId: String)(implicit ctx:ExecutionContext): Future[Boolean] =
    queryDelete(index, termQuery("_parent", parentId), Some(parentId))
      
}