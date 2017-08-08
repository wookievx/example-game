package services

import akka.actor._
import akka.typed.ActorRef
import com.example.model.units.UnitState._
import com.example.model.units.{UnitInstance, UnitState}

import scala.collection.{SortedSet, mutable}
import scala.language.implicitConversions
import scala.reflect.ClassTag

package object game extends LowPriorityMonoidImplicits {

  type MSeq[T] = mutable.Seq[T]
  val MSeq = mutable.Seq

  type UnsafeActorRef[T] = akka.actor.ActorRef

  private val utilLogger = play.api.Logger(getClass)

  class UnsafeWrapper[T](ref: ActorRef[T])(implicit ct: ClassTag[T]) extends Actor {
    override def receive: Receive = {
      case ct(c) =>
        utilLogger.info(s"Wrapper over typed actor received message: $c")
        ref ! c
      case e =>
        utilLogger.warn(s"Type checking in wrapper over typed actor failed with message: $e, class tag is for class: ${ct.runtimeClass.getSimpleName}")
    }
  }

  class UnsafeNarrowingWrapper[T, U](ref: ActorRef[T], fun: U => T)(implicit ct: ClassTag[U]) extends Actor {
    override def receive: Receive = {
      case ct(u) =>
        utilLogger.info(s"Wrapper over typed actor received message: $u")
        ref ! fun(u)
      case e =>
        utilLogger.warn(s"Type checking in wrapper over typed actor failed with message: $e, class tag is for class: ${ct.runtimeClass.getSimpleName}")
    }
  }

  class Converter[T](private val ref: ActorRef[T]) extends AnyVal {
    def toUnsafeActor(implicit system: ActorSystem, classTag: ClassTag[T]): UnsafeActorRef[T] = {
      val props = Props {
        new UnsafeWrapper(ref)
      }
      system.actorOf(props)
    }
    def toNarrowingActor[U](fun: U => T)(implicit system: ActorSystem, classTag: ClassTag[U]): UnsafeActorRef[U] = {
      val props = Props {
        new UnsafeNarrowingWrapper(ref, fun)
      }
      system.actorOf(props)
    }
  }

  implicit def toConverter[T](ref: ActorRef[T]): Converter[T] = new Converter(ref)

  trait Monoid[T] {
    def add(a: T, b: T): T
  }

  implicit def unitSortedSetMonoid = new Monoid[SortedSet[UnitInstance]] {
    private def selectMoreImportant(l: UnitState, r: UnitState): UnitState = if (l == r) l else {

      def minimumOf2(f: UnitState, s: UnitState): UnitState = f.withRemainingHealth(math.min(f.remainingHealth, s.remainingHealth))

      (l, r) match {
        case (_: Moving, default) => default
        case (default, _: Moving) => default
        case (_: Blocked, default) => default
        case (default, _: Blocked) => default
        case (d: Attacked, default) => minimumOf2(default, d)
        case (default, d: Attacked) => minimumOf2(default, d)
        case (default, _: Attacking) => default
        case (_: Attacking, default) => default
        case (Dead, Dead) => Dead
      }
    }

    override def add(a: SortedSet[UnitInstance], b: SortedSet[UnitInstance]): SortedSet[UnitInstance] = {
      val aMap = a.map(u => u.instanceID -> u).toMap
      val tmpMap = aMap ++ b.map { u => u.instanceID -> {
          val state = aMap.get(u.instanceID).map(_.unitState).map(selectMoreImportant(_, u.unitState)).getOrElse(u.unitState)
          u.copy(unitState = state)
        }
      }
      tmpMap.values.to[SortedSet]
    }
  }


  implicit def optionMonoid[T: Monoid] = new Monoid[Option[T]] {
    override def add(a: Option[T], b: Option[T]): Option[T] = {
      val combined = for {
        f <- a
        s <- b
      } yield f |+| s
      combined.orElse(a).orElse(b)
    }
  }

  implicit def mapMonoid[K, V](implicit ev: Monoid[V]) = new Monoid[Map[K, V]] {
    override def add(a: Map[K, V], b: Map[K, V]): Map[K, V] = a ++ { b.map { case (k, v) => k -> (a.get(k) |+| v)}}
  }

  implicit def monoidToExtension[T: Monoid](elem: T): MonoidExtension[T] = new MonoidExtension(elem)

  class MonoidExtension[T](private val elem: T) extends AnyVal {
    @inline
    def |+|(other: T)(implicit ev: Monoid[T]): T = ev.add(elem, other)
    @inline
    def |+|(other: Option[T])(implicit ev: Monoid[T]): T = other match {
      case Some(t) => ev.add(elem, t)
      case None => elem
    }
    @inline
    def |+|[U](other: U)(implicit ev: T <:< Option[U], monoid: Monoid[U]): U = other |+| ev(elem)
  }

  @inline
  implicit def toEitherExtension[T](elem: T): EitherExtension[T] = new EitherExtension(elem)

  class EitherExtension[T](private val elem: T) extends AnyVal {
    @inline
    def toLeft[R]: Left[T, R] = Left(elem)
    @inline
    def toRight[L]: Right[L, T] = Right(elem)
  }

}
