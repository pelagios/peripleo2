package harvesting.crosswalks.people

import services.item.ItemRecord

/** Simple crosswalk for data about people. Not a crosswalk for data about simple people. **/
object SimplePeopleCrosswalk {
  
  def fromJson(record: String): Option[ItemRecord] = {
    
    play.api.Logger.info(record)
    
    None
  }
  
}