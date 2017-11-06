package services.visit

import services.visit.info.SelectionInfo

case class VisitStats(
  total      : Long, 
  topItems   : Seq[(VisitStats.TopSelected, Long)], 
  topDatasets: Seq[(VisitStats.TopSelected, Long)],
  topSearches: Seq[(String, Long)])
  
  
object VisitStats {
  
  case class TopSelected private[visit] (str: String) {
        
    val identifier = str.substring(0, str.indexOf(SelectionInfo.SEPARATOR))
    
    val title = str.substring(str.indexOf(SelectionInfo.SEPARATOR) + 1)

  }

}