package harvesting.crosswalks

import java.io.InputStream
import org.joda.time.{ DateTime, DateTimeZone }
import org.pelagios.Scalagios
import org.pelagios.api.dataset.Dataset
import services.item._
import services.item.reference.UnboundReference

object PelagiosVoIDCrosswalk extends PelagiosCrosswalk {

  /** Returns a flat list of all datasets below this one in the hierarchy
    *
    * TODO propagate the following properties from rootset to subsets:
    * - publisher
    * - license
    */
  def flattenHierarchy(rootset: Dataset): Seq[Dataset] =
    if (rootset.subsets.isEmpty) Seq(rootset)
    else rootset +: rootset.subsets.flatMap(flattenHierarchy)

  def convertDataset(rootset: Dataset, voidURL: String): Seq[ItemRecord] =
    flattenHierarchy(rootset).map { d =>
      val parentHierarchy =  {
        val parents = findParents(d).reverse
        if (parents.isEmpty) None
        else Some(PathHierarchy(parents.map(d => (d.uri -> d.title))))
      }

      ItemRecord(
        d.uri,
        Seq(d.uri),
        DateTime.now().withZone(DateTimeZone.UTC),
        None, // lastChangedAt
        d.title,
        None, // isInDataset
        parentHierarchy,
        d.subjects.map(Category(_)),
        d.description.map(d => Seq(Description(d))).getOrElse(Seq.empty[Description]),
        d.homepage,
        d.license,
        Seq.empty[Language],
        Seq.empty[Depiction],
        None, // TODO geometry
        None, // TODO representative point
        None, // temporal_bounds
        Seq.empty[Name],
        Seq.empty[Link],
        rootset.publisher,
        Some(voidURL))
    }

  def fromDatasets(rootDatasets: Seq[Dataset], voidURL: String): Seq[ItemRecord] =
    rootDatasets.flatMap(convertDataset(_, voidURL))

}
