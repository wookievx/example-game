package services.game

import scala.collection.SortedSet

trait LowPriorityMonoidImplicits {

  implicit def monoidSetInstance[T] = new Monoid[Set[T]] {
    override def add(a: Set[T], b: Set[T]): Set[T] = a ++ b
  }

  implicit def sortedSetMonoid[T] = new Monoid[SortedSet[T]] {
    override def add(a: SortedSet[T], b: SortedSet[T]): SortedSet[T] = a ++ b
  }

}
