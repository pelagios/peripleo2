package services.visit

case class VisitStats(
  total      : Long, 
  topItems   : Seq[(String, Long)], 
  topDatasets: Seq[(String, Long)],
  topSearches: Seq[(String, Long)])