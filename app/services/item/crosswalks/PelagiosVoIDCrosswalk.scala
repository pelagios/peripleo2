package services.item.crosswalks

import java.io.InputStream
import org.joda.time.{ DateTime, DateTimeZone }
import org.pelagios.Scalagios
import org.pelagios.api.dataset.Dataset
import services.item._

object PelagiosVoIDCrosswalk {
  
  /** Returns a flat list of all datasets below this one in the hierarchy 
    *
    * TODO propagate the following properties from rootset to subsets:
    * - publisher
    * - license
    */
  def flattenHierarchy(rootset: Dataset): Seq[Dataset] =
    if (rootset.subsets.isEmpty) Seq(rootset)
    else rootset +: rootset.subsets.flatMap(flattenHierarchy)
    
  def findParents(dataset: Dataset, parentURIs: Seq[String] = Seq.empty[String]): Seq[String] = 
    dataset.isSubsetOf match {
      case Some(parent) => parent.uri +: findParents(parent)
      case _ => parentURIs
    }
    
  def convertDataset(rootset: Dataset): Seq[(Item, Seq[Reference])] = {
        
    val datasets = flattenHierarchy(rootset).map { d =>
      val parentHierarchy =  {
        val parents = findParents(d).reverse
        if (parents.isEmpty) None
        else Some(PathHierarchy(parents))
      }
      
    Item(
      Seq(d.uri),
      ItemType.DATASET,
      d.title,
      Some(DateTime.now().withZone(DateTimeZone.UTC)),
      None, // last_changed_at
      d.subjects.map(Category(_)),
      Seq.empty[PathHierarchy], // is_in_dataset
      parentHierarchy,
      d.description.map(d => Seq(Description(d))).getOrElse(Seq.empty[Description]),
      d.homepage,
      d.license,
      Seq.empty[Language],
      None, // TODO geometry
      None, // TODO representative point
      None, // temporal_bounds
      Seq.empty[String], // periods
      Seq.empty[Depiction])
    }
    
    // We need to add an empty set of references, so it's compatible
    // with the generic ItemService import interface
    datasets.map((_, Seq.empty[Reference]))
  }
 
  def fromRDF(filename: String): InputStream => Seq[(Item, Seq[Reference])] =
    { stream: InputStream =>
      Scalagios.readVoID(stream, filename).flatMap(convertDataset).toSeq }
  
  def fromDatasets(rootDatasets: Seq[Dataset]): Seq[(Item, Seq[Reference])] =
    rootDatasets.flatMap(convertDataset)
  
}