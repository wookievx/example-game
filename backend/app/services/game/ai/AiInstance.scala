package services.game.ai

import akka.typed._
import akka.typed.scaladsl.Actor
import com.example.model.Command.CreateUnit
import com.example.model.GameResponse._
import com.example.model._
import com.example.model.units.UnitObject._
import com.example.model.units.{UnitInstance, UnitObject}
import models.IdentifiedCommand
import services.game.GameRepository._
import services.game.Ongoing
import services.game.GameHandler._
import services.game._

import scala.collection.SortedSet
import scala.concurrent.duration._

trait AiInstance {
  import AiInstance._
  def id: OwnerId
  protected def respondTo: ActorRef[GameMessage]
  protected def unitCost(unitObject: UnitObject): Int
  protected def availableUnits: Set[UnitObject]
  def behavior(gameState: Ongoing): Behavior[AIMessage] = Actor.deferred { ctx =>
    ctx.schedule(100.millis, ctx.self, NextAITick.toRight)
    behaviorImpl(gameState)
  }

  private def behaviorImpl(gameState: Ongoing): Behavior[AIMessage] = Actor.immutable { (ctx, msg) =>
    msg match {
      case Right(NextAITick) =>
        val bestUnit = assessBestUnitToSpawn(gameState)
        bestUnit.foreach(u => respondTo ! IdentifiedCommand(id, id, CreateUnit(u)).toEvent)
        ctx.schedule(100.millis, ctx.self, NextAITick.toRight)
        Actor.same
      case Left(r: UnitResponse) =>
        behaviorImpl(updateUnits(gameState, r))
      case Left(PlayerStateResponse(oid, state, _)) =>
        behaviorImpl(updatePlayerState(gameState, oid, state))
      case Left(CommandResponse(_, _)) =>
        Actor.same
      case Left(GameEnded) => //ignored
        Actor.same
    }

  }


  private def updateUnits(state: Ongoing, response: UnitResponse): Ongoing = {
    val units = for {
      (l, m) <- state.units
    } yield {
      l -> {
        val updatedMap = m.map(i => i.instanceID -> i).toMap ++ response.instances
          .iterator.collect{ case i if i.y == l => i.instanceID -> i }
        updatedMap.values.to[SortedSet]
      }
    }
    state.copy(units = units)
  }

  private def updatePlayerState(state: Ongoing, pid: OwnerId, ps: PlayerState.StateDiff): Ongoing = if (pid == state.player1) {
    state.copy(player1State = state.player1State withDiff ps)
  } else {
    state.copy(player2State = state.player2State withDiff ps)
  }

  private def assessBestUnitToSpawn(state: Ongoing): Option[UnitObject] = {
    val currentGold = state.player1State.gold
    val currentLane = state.player1State.currentLane
    val enemyUnitsInLane = state.units(currentLane).collect {
      case UnitInstance(_, oid, uo, _, _, us) if oid != id => uo -> us
    }
    val orderedUnits = enemyUnitsInLane.toSeq.sortBy { case (u, s) => s.remainingHealth / u.health}.zipWithIndex
    val unitTypeWeights = orderedUnits.groupBy(_._1._1).mapValues { seq =>
      seq.map(_._2).sum
    }
    val cheapestUnit = availableUnits.minBy(unitCost)
    val optCheapestUnit = Option(cheapestUnit).filter(unitCost(_) < currentGold)
    if (unitTypeWeights.nonEmpty) {
      val (unit, _) = unitTypeWeights.maxBy(_._2)
      unit match {
        case _: Archer => availableUnits.find(s => unitCost(s) < currentGold && s.name == swordsman.name).orElse(optCheapestUnit)
        case _: Swordsman => availableUnits.find(s => unitCost(s) < currentGold && s.name == wizard.name).orElse(optCheapestUnit)
        case _: Wizard => availableUnits.find(s => unitCost(s) < currentGold && s.name == archer.name).orElse(optCheapestUnit)
      }
    } else {
      optCheapestUnit
    }
  }
}

object AiInstance {
  type AIMessage = Either[GameResponse, NextAITick.type]
  object NextAITick
}
