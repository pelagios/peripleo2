package harvesting.crosswalks

import org.scalatestplus.play._
import play.api.test._
import play.api.test.Helpers._
import org.pelagios.api.dataset.Dataset

object TestCrosswalk extends PelagiosCrosswalk { }

class PelagiosCrosswalkSpec extends PlaySpec {

  val A1 = Dataset("http://www.example.com/datasets/0/A/A1", "A1")
  val A2 = Dataset("http://www.example.com/datasets/0/A/A2", "A2")
  val B1 = Dataset("http://www.example.com/datasets/0/B/B1", "B1")
  val B2 = Dataset("http://www.example.com/datasets/0/B/B2", "B2")
  val B3 = Dataset("http://www.example.com/datasets/0/B/B3", "B3")

  val A = Dataset("http://www.example.com/datasets/0/A", "A", subsets=Seq(A1, A2))
  val B = Dataset("http://www.example.com/datasets/0/B", "B", subsets=Seq(B1, B2, B3))

  val ROOT = Dataset("http://www.example.com/datasets/0", "Root", subsets=Seq(A, B))
    
  "findSubsetRecursive" should {
    
    "find child datasets at both levels" in {
      val matchLvl1 = TestCrosswalk.findSubsetRecursive("http://www.example.com/datasets/0/B", ROOT)
      val matchLvl2 = TestCrosswalk.findSubsetRecursive("http://www.example.com/datasets/0/A/A2", ROOT)
      
      matchLvl1.isDefined mustEqual true
      matchLvl2.isDefined mustEqual true
      
      matchLvl1.get.title mustEqual "B"
      matchLvl2.get.title mustEqual "A2"
    }
    
  }
  
}