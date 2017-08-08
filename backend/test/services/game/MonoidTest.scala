package services.game

import org.scalatest.Matchers
import org.scalatest._

import scala.collection.SortedSet

class MonoidTest extends FlatSpec with Matchers {

  "Map |+| operator" should "work recursively" in {
    val m1 = Map(1 -> SortedSet("a", "b", "c"), 2 -> SortedSet("3", "4", "5"))
    val m2 = Map(1 -> SortedSet("c", "d", "e"), 3 -> SortedSet("1", "2", "6"))
    val res = Map(1 -> SortedSet("a", "b", "c", "d", "e"), 2 -> SortedSet("3", "4", "5"), 3 -> SortedSet("1", "2", "6"))
    m1 |+| m2 should equal(res)
  }


}
