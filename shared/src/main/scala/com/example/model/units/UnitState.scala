package com.example.model.units

import boopickle.Default._
import boopickle.Pickler

import scala.concurrent.duration._
import scala.language.implicitConversions
sealed trait UnitState {
  protected def cdMillis: Long
  def remainingHealth: Int
  def canAttack: Boolean = cdMillis < System.currentTimeMillis()
  def withRemainingHealth(health: Int): UnitState
}

object UnitState {


  case class Moving(remainingHealth: Int, cdMillis: Long = System.currentTimeMillis()) extends UnitState {
    override def withRemainingHealth(health: Int): UnitState = copy(remainingHealth = health)
  }
  case class Blocked(remainingHealth: Int, cdMillis: Long = System.currentTimeMillis()) extends UnitState {
    override def withRemainingHealth(health: Int): UnitState = copy(remainingHealth = health)
  }
  case class Attacking(damageDealt: Int, remainingHealth: Int, cdMillis: Long = System.currentTimeMillis()) extends UnitState {
    override def withRemainingHealth(health: Int): UnitState = copy(remainingHealth = health)
  }
  case class Attacked(damageReceived: Int, remainingHealth: Int, cdMillis: Long = System.currentTimeMillis()) extends UnitState {
    override def withRemainingHealth(health: Int): UnitState = copy(remainingHealth = health)
  }
  case object Dead extends UnitState {
    override def remainingHealth: Int = 0
    override protected def cdMillis: Long = 0
    override def withRemainingHealth(health: Int): UnitState = this
  }




  class UnitObjectExtension(private val unitObj: UnitObject) extends AnyVal {
    def moving: Moving = Moving(unitObj.health)
    def blocked: Blocked = Blocked(unitObj.health)
    @deprecated
    def attacking(dmg: Int): Attacking = Attacking(dmg, unitObj.health)
    @deprecated
    def attacked(health: Int): Attacked = Attacked(0, health)
    def dead = Dead
  }

  trait UnitFSM {
    def toMoving: UnitState
    def toBlocked: UnitState
    /**
      * This method should only be called after checking whether unit can attack, in other case, the contract is broken
      */
    def toAttacking(dmg: Int, coolDown: FiniteDuration): UnitState
    def toAttacked(dmg: Int): UnitState
  }

  @inline
  class SimpleUnitFSM(private val state: UnitState) extends AnyVal {
    def toMoving: UnitState = Moving(state.remainingHealth, state.cdMillis)
    def toBlocked: UnitState = Blocked(state.remainingHealth, state.cdMillis)
    def toAttacking(dmg: Int, coolDown: FiniteDuration): UnitState = Attacking(dmg, state.remainingHealth, System.currentTimeMillis() + coolDown.toMillis)
    def toAttacked(dmg: Int): UnitState = state match {
      case s if s.remainingHealth < dmg => Dead
      case Attacked(r, remainingHealth, cd) => Attacked(r + dmg, remainingHealth - dmg, cd)
      case s => Attacked(dmg, s.remainingHealth - dmg, s.cdMillis)
    }
  }


  implicit def toExtension(unitObject: UnitObject): UnitObjectExtension = new UnitObjectExtension(unitObject)
  implicit def toAdvancedUnitFSM(state: UnitState): SimpleUnitFSM = new SimpleUnitFSM(state)

  lazy implicit val pickler: Pickler[UnitState] = generatePickler[UnitState]
}
