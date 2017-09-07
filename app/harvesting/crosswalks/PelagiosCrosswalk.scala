package harvesting.crosswalks

import org.pelagios.api.dataset.Dataset

trait PelagiosCrosswalk {
  
  def findParents(dataset: Dataset, parents: Seq[Dataset] = Seq.empty[Dataset]): Seq[Dataset] =
    dataset.isSubsetOf match {
      case Some(parent) => parent +: findParents(parent)
      case _ => parents
    }
  
  def findSubsetRecursive(uri: String, seed: Dataset): Option[Dataset] = {
    if (seed.uri == uri) {
      Some(seed)
    } else if (seed.subsets.isEmpty) {
      None
    } else {
      seed.subsets.map(findSubsetRecursive(uri, _)).find(_.isDefined).flatten
    }
  }
  
}