package harvesting.crosswalks

import java.io.File
import kantan.csv.ops._
import org.joda.time.DateTime
import services.item._
import services.item.reference._
import scala.io.Source

/** (Currently prototypical) CSV crosswalk. 
  *
  * TODO Later, this should become configurable via a config file; and, even later, an import wizard dialog. 
  * TODO we should re-organize the loaders so that we can support streaming loaders can take a file as input
  * (not just a single line). This will make the use of inherently streamable formats (such as CSV) easier with
  * libraries that can already handle the streaming (like KantanCSV).
  */
object CSVCrosswalk {
  
  private def guessDelimiter(line: String): Char = {
    // This test is pretty trivial but seems to be applied elsewhere (see e.g.
    // http://stackoverflow.com/questions/14693929/ruby-how-can-i-detect-intelligently-guess-the-delimiter-used-in-a-csv-file)
    // Simply count the most-used candidate
    val choices = Seq(',', ';', '\t', '|')
    val ranked = choices
      .map(char => (char, line.count(_ == char)))
      .sortBy(_._2).reverse
      
    ranked.head._1
  }
      
  def fromCSV(file: File, inDataset: PathHierarchy): String => Option[(ItemRecord, Seq[UnboundReference])] = {
    val first = Source.fromFile(file).getLines().take(1).next()
    val delimiter = guessDelimiter(first)
    
    // TODO currently a hard-wired hack for the million musical tweets dataset
    def parseRow(row: String): Option [(ItemRecord, Seq[UnboundReference])] = {
      val fields = row.asCsvReader[Seq[String]](delimiter, header = false).toIterator.next.get
      
      val id = fields(0)
      val geonamesId = fields(1)
      val tweetId = fields(2)
      val timestamp = fields(6)
      val artist = fields(11)
      val track = fields(13)

      val uri = inDataset.path.head._2 + ":" + id
      
      val record = ItemRecord(
        uri,
        Seq(uri),
        DateTime.now,
        None,
        artist + ": " + track,
        Some(inDataset),
        None,
        Seq(Category(artist)),
        Seq.empty[Description],
        Some("http://twitter.com/status/" + tweetId),
        None,
        Seq.empty[Language],
        Seq.empty[Depiction],
        None,
        None,
        None, // TODO temporal bounds
        Seq.empty[Name],
        Seq.empty[String], Seq.empty[String])
        
      val reference = UnboundReference(
        uri,
        ReferenceType.PLACE,
        "http://sws.geonames.org/" + geonamesId,
        Some(Relation.LOCATION),
        None, None, None)
          
      Some((record, Seq(reference)))
    }
    
    parseRow
  }
  
}