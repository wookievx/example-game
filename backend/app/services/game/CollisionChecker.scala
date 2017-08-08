package services.game

import com.example.config.GameConfig
import com.example.model.OwnerId
import com.example.model.units.UnitObject.Swordsman
import com.example.model.units.UnitState._
import com.example.model.units._

import scala.collection.SortedSet
import scala.util.Random

trait CollisionChecker {
  private val logger = play.api.Logger(classOf[CollisionChecker])
  val gameConfig: GameConfig

  import gameConfig._

  def checkUnitRange(ids: Set[OwnerId], lane: Int, instance: UnitInstance, range: Int, state: Ongoing): Option[UnitInstance] = {
    val searchRange = state.units(lane).collect { case ui@UnitInstance(_, i, uo, x, _, _) if ids contains i => ui.copy(x = x - unitDimensions(uo)._1 / 2) }
    val u = searchRange.range(instance.x, instance.x + range).headOption
    u.map { ui => ui.copy(x = ui.x + unitDimensions(ui.unitObject)._1 / 2) }
  }

  private def checkBaseAttacks(state: Ongoing): Ongoing = {

    def attacking(s: Ongoing, attacker: OwnerId): Ongoing = {
      val a = for {
        (l, m) <- s.units
      } yield {
        l -> {
          m.collect {
            case i@UnitInstance(_, id, uo, x, _, st) if x + uo.attackRange > width && id == attacker && st.canAttack =>
              //inflict half of the dmg to attacker
              val tmpDamaged = i.unitState.toAttacked(uo.damage / 2)
              tmpDamaged match {
                case Dead => i.copy(unitState = Dead)
                case _ => i.copy(unitState = tmpDamaged.toAttacking(uo.damage, uo.attackInterval))
              }
            case i@UnitInstance(_, id, uo, x, _, st) if x + uo.attackRange > width && id == attacker =>
              i.copy(unitState = st.toBlocked)
          }
        }
      }
      state.copy(units = a)
    }

    def dmgDealt(attackState: Ongoing): Int = {
      (for ((_, m) <- attackState.units; UnitInstance(_, _, _, _, _, Attacking(dmg, _, _)) <- m) yield dmg).sum
    }

    val attackingLeft = attacking(state, state.player1)
    val attackingRight = reverseState(attacking(reverseState(state), state.player2))
    val leftDmgDealt = dmgDealt(attackingLeft)
    val rightDmgDealt = dmgDealt(attackingRight)
    val leftPlayerState = state.player1State.withBaseHealth(_ - rightDmgDealt)
    val rightPlayerState = state.player2State.withBaseHealth(_ - leftDmgDealt)
    state.copy(player1State = leftPlayerState, player2State = rightPlayerState, units = attackingLeft.units |+| attackingRight.units)
  }

  def reverseState(state: Ongoing): Ongoing = {
    val mapped = for {
      (l, m) <- state.units
    } yield l -> m.map { i => i.copy(x = width - i.x) }
    state.copy(units = mapped)
  }

  def isPositionFree(lane: Int, x: Int, unitObject: UnitObject, state: Ongoing): Boolean = {
    val (width, _) = unitDimensions(unitObject)
    state.units(lane).range(x - width / 2, x + width / 2).isEmpty
  }

  def handleBlocking(fid: OwnerId, state: Ongoing): Ongoing = {
    val movements = for {
      (l, m) <- state.units
    } yield {
      l -> {
        for {
          u <- m if u.ownerId == fid
          _ <- checkUnitRange(Set(state.player1, state.player2), l, u, unitDimensions(u.unitObject)._1 / 2, state)
        } yield {
//          logger.warn(s"Detected blocking: {$l, ${u.ownerId}, ${u.x}, ${u.unitObject}}")
          u.copy(unitState = u.unitState.toBlocked)
        }
      }
    }
    state.copy(units = movements)
  }

  private def handleAttacks(attacking: OwnerId, attacked: OwnerId, state: Ongoing): Ongoing = {
    val affected = for {
      (l, m) <- state.units
    } yield {
      l -> (for {
        u <- m if u.ownerId == attacking && u.unitState.canAttack
        attacked <- checkUnitRange(Set(attacked), l, u, u.unitObject.attackRange, state)
      } yield {
//        logger.warn(s"Detected attack(lane: $l): {${attacking.id}, ${u.x}, ${u.unitObject}} x {${attacked.x}, ${attacked.unitObject}}")
        val dmg = UnitObject.damageMultiplication(attacked.unitObject.armorType, u.unitObject.damageType) * u.unitObject.damage
        val nextAttackerState = u.unitState.toAttacking(dmg.intValue(), u.unitObject.attackInterval)
        Set(u.copy(unitState = nextAttackerState), attacked.copy(unitState = attacked.unitState.toAttacked(dmg.intValue())))
      }).flatten.to[SortedSet]
    }
    state.copy(units = affected)
  }


  def handleInteractions(state: Ongoing): Ongoing = {
    val leftPlayerMovements = handleBlocking(state.player1, state)
    val leftPlayerAttacks = handleAttacks(state.player1, state.player2, state)
//    leftPlayerAttacks.units.values.flatten.foreach(u => logger.warn(s"{${u.ownerId}, ${u.unitObject}, ${u.x}, ${u.unitState}}"))

    val reversed = reverseState(state)
    val rightPlayerMovements = handleBlocking(state.player2, reversed)
    val rightPlayerAttacks = handleAttacks(state.player2, state.player1, reversed)
    val restored = reverseState(reversed.copy(units = rightPlayerMovements.units |+| rightPlayerAttacks.units))
//    rightPlayerAttacks.units.values.flatten.foreach(u => logger.warn(s"{${u.ownerId}, ${u.unitObject}, ${u.x}, ${u.unitState}}"))

    val afterU2UInteractions = state.copy(units = leftPlayerMovements.units |+| restored.units |+| leftPlayerAttacks.units)
    val attackingBase = checkBaseAttacks(state)
    attackingBase.copy(units = attackingBase.units |+| afterU2UInteractions.units)
  }


}


