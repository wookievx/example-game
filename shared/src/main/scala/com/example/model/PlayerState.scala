package com.example.model

import boopickle.Default._
import com.example.config.GameConfig
import com.example.model.PlayerState.StateDiff
import com.example.model.units.UnitInstance

import scala.collection.immutable.Queue

case class PlayerState(currentLane: Int = 0, unitCount: Int = 0, baseHealth: Int, gold: Int, productionQueue: Queue[UnitInstance] = Queue.empty) {
  def withUnitCount(modifier: Int => Int): PlayerState = copy(unitCount = modifier(unitCount))
  def withBaseHealth(modifier: Int => Int): PlayerState = copy(baseHealth = modifier(baseHealth))
  def withGold(modifier: Int => Int): PlayerState = copy(gold = modifier(gold))
  def withUnitInQueue(unit: UnitInstance): PlayerState = copy(unitCount = unitCount + 1, productionQueue = productionQueue.enqueue(unit))
  def dequeUnit: PlayerState = {
    val (u, q) = productionQueue.dequeue
    copy(productionQueue = q)
  }
  def withLane(modifier: Int => Int): PlayerState = copy(currentLane = modifier(currentLane))

  def diff(other: PlayerState): StateDiff = {
    val dequeue = math.max(productionQueue.size - other.productionQueue.size, 0)
    StateDiff(
      unitCountDiff = unitCount - other.unitCount,
      healthDiff = baseHealth - other.baseHealth,
      goldDiff = gold - other.gold,
      dequeue = dequeue,
      enqueued = productionQueue.take(dequeue).toArray
    )
  }

  def withDiff(diff: StateDiff): PlayerState = copy(
    unitCount = unitCount + diff.unitCountDiff,
    baseHealth = baseHealth + diff.healthDiff,
    gold = gold + diff.goldDiff,
    productionQueue = {
      val d = productionQueue.drop(diff.dequeue)
      (diff.enqueued.reverse foldLeft d) { (q, i) => q.enqueue(i) }
    }
  )
}

object PlayerState {
  implicit val pickler: Pickler[PlayerState] = generatePickler

  case class StateDiff(unitCountDiff: Int, healthDiff: Int, goldDiff: Int, dequeue: Int, enqueued: Array[UnitInstance])

  def fromGameConfig(config: GameConfig): PlayerState = PlayerState(
    baseHealth = config.baseHealth,
    gold = config.startGold,
  )
}
