package bundlepricing.util

import scalaz.syntax.semigroup._
import scalaz.{Equal, NonEmptyList, Order, Semigroup}

/** An immutable Map with at least one entry */
class NonEmptyMap[K,V] private(raw: Map[K,V]) {
  def get(k: K): Option[V] = raw.get(k)

  /** Remove an element from the map. Returns None if the removed element was the only one. */
  def -(k: K): Option[NonEmptyMap[K,V]] =
    if (raw.size > 1) Some(new NonEmptyMap(raw - k))
    else if (raw.contains(k)) None
    else Some(this)

  /** Add an element to the map, or replace an existing mapping */
  def +(pair: (K,V)): NonEmptyMap[K,V] =
    new NonEmptyMap(raw + pair)

  def ++(other: Map[K,V]): NonEmptyMap[K,V] = new NonEmptyMap(raw ++ other)
  def ++(other: NonEmptyMap[K,V]): NonEmptyMap[K,V] = new NonEmptyMap(raw ++ other.toMap)

  def contains(k: K): Boolean = raw.contains(k)

  def forall(p: ((K,V)) => Boolean) = raw.forall(p)
  def exists(p: ((K,V)) => Boolean) = raw.exists(p)

  def foldLeft[B](z: B)(f: (B, (K,V)) => B): B = raw.foldLeft(z)(f)
  def foldMap1[B](f: ((K,V)) => B)(implicit F: Semigroup[B]): B =
    raw.tail.foldLeft[B](f(raw.head)) { case (sum, pair) => F.append(sum, f(pair)) }

  def toMap: Map[K,V] = raw
  def toNEL: NonEmptyList[(K,V)] = (raw.toList: @unchecked) match { case head :: tail => NonEmptyList.nel(head, tail) }
  def toList: List[(K,V)] = raw.toList

  override def toString: String =
    raw.mkString("NonEmptyMap(", ",", ")")
}

object NonEmptyMap {
  def apply[K,V](one: (K,V), others: (K,V)*): NonEmptyMap[K,V] =
    new NonEmptyMap[K,V](Map(others: _*) + one)

  implicit def semigroup[K,V:Semigroup]: Semigroup[NonEmptyMap[K,V]] =
    Semigroup.instance[NonEmptyMap[K,V]] {
      (map1, map2) =>
        map1.foldLeft(map2) {
          case (m, (k, v1)) =>
            m + (k -> m.get(k).map(v2 => v1 |+| v2).getOrElse(v1))
        }
    }

  implicit def equal[K:Order,V:Equal]: Equal[NonEmptyMap[K,V]] =
    scalaz.std.map.mapEqual[K,V].contramap[NonEmptyMap[K,V]](_.toMap)
}