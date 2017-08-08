package services.game

import com.example.config.GameConfig
import com.example.model._
import com.example.model.units.UnitInstance
import com.example.model.units.UnitState._

import scala.util.Random

trait UnitHandler extends CollisionChecker {
  val gameConfig: GameConfig
  private val logger = play.api.Logger(getClass)

  def tickForUnits(gameState: Ongoing): (Ongoing, Ongoing) = {
    val (movedUnits, stateDifference) = tickUnitActions(gameState)

    def dequeueIfPossible(s: Ongoing, playerState: PlayerState): (PlayerState, Ongoing) = playerState.productionQueue.headOption.collect {
      case i@UnitInstance(_, id, obj, _, lane, _) if isPositionFree(lane, startPosition(id, movedUnits), obj, movedUnits) =>
        val sp = startPosition(id, movedUnits)
        playerState.dequeUnit -> s.copy(units = s.units + (lane -> (s.units(lane) + i.copy(x = sp))))
    }.getOrElse(playerState -> s)

    val (p1m, s1m) = dequeueIfPossible(stateDifference, movedUnits.player1State)
    val (p2m, s2m) = dequeueIfPossible(s1m, movedUnits.player2State)
    s2m.copy(player1State = p1m, player2State = p2m, units = movedUnits.units |+| s2m.units) -> s2m
  }

  protected def startPosition(id: OwnerId, state: Ongoing): Int

  private def moveUnits(gameState: Ongoing): Ongoing = {
    val movedUnits = for {
      (l, m) <- gameState.units
    } yield {
      l -> {
        for (i <- m) yield {
          i.unitState match {
            case Moving(_, _) if i.ownerId == gameState.player1 =>
              i.copy(x = i.x + i.unitObject.movementSpeed)
            case Moving(_, _) if i.ownerId == gameState.player2 =>
              i.copy(x = i.x - i.unitObject.movementSpeed)
            case _ =>
              i
          }
        }
      }
    }
    gameState.copy(units = movedUnits)
  }

  def tickUnitActions(gameState: Ongoing): (Ongoing, Ongoing) = {
    val passivated = passivateUnits(gameState)
    val handleBlockingAndAttacks = handleInteractions(passivated)
//    logger.warn(s"${handleBlockingAndAttacks.units.valuesIterator.flatten.collect {
//      case UnitInstance(_, _, obj, x, _, Blocked(h, _)) => (obj, x, h)
//    }.mkString("[", ", ", "]")}")
    val allUnits = handleBlockingAndAttacks.copy(units = passivated.units |+| handleBlockingAndAttacks.units)
    moveUnits(allUnits) -> handleBlockingAndAttacks
  }

  private def passivateUnits(gameState: Ongoing): Ongoing = {
    val units = for {
      (l, m) <- gameState.units
    } yield {
      l -> {
        for (i <- m) yield i.copy(unitState = i.unitState.toMoving)
      }
    }
    gameState.copy(units = units)
  }

  def removeDead(gameState: Ongoing): Ongoing = {
    val alive = for {
      (l, m) <- gameState.units
    } yield {
      l -> {
        val alive = m.filter(_.unitState != Dead)
        alive
      }
    }
    val dead = for {
      (_, m) <- gameState.units
      u <- m if u.unitState == Dead
    } yield u
    val player1DeadUnits = dead.collect { case i if i.ownerId == gameState.player1 => i}.size
    val player2DeadUnits = dead.collect { case i if i.ownerId == gameState.player2 => i}.size

    gameState.copy(player1State = gameState.player1State.withUnitCount(_ - player1DeadUnits),
                   player2State = gameState.player2State.withUnitCount(_ - player2DeadUnits),
                   units = alive)
  }

}
