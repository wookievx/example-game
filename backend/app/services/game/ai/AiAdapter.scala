package services.game.ai

import akka.actor.ActorSystem
import akka.typed.scaladsl.Actor
import akka.typed.{ActorRef, Behavior, Terminated}
import com.example.config.GameConfig
import com.example.model.units.UnitObject
import com.example.model.{GameResponse, OwnerId}
import services.game.GameRepository._
import services.game._
import services.game.ai.AiAdapter._
import services.game.ai.AiInstance.{AIMessage, NextAITick}

import scala.concurrent.duration._

class AiAdapter(gameConfig: GameConfig, gameRepositoryInstance: ActorRef[GameMessage])(implicit utilitySystem: ActorSystem) { outer =>

  private def logger = play.api.Logger(getClass)

  private def newAiInstance(ownerId: OwnerId) = new AiInstance {
      override protected def availableUnits: Set[UnitObject] = gameConfig.availableUnits
      override protected def unitCost(unitObject: UnitObject): Int = gameConfig.unitValue(unitObject)
      override def id: OwnerId = ownerId
      override protected def respondTo: ActorRef[GameMessage] = gameRepositoryInstance
  }

  def mainBehavior(id: OwnerId): Behavior[AIAdapterMessage] = {
    val aiInstance = newAiInstance(id)

    def wrapping(ref: ActorRef[AIMessage]): Behavior[AIAdapterMessage] = Actor.immutable[AIAdapterMessage] { (ctx, msg) =>
      ctx.watch(ref)
      msg match {
        case Left(r) => ref ! Left(r); Actor.same
        case Right(Kill(_)) => ctx.stop(ref); Actor.stopped
        case Right(m) =>
          logger.error(s"Illegal message for initialised ai: $m")
          Actor.stopped
      }
    } onSignal {
      case (_, t@Terminated(_)) if t.wasFailed =>
        logger.error(s"AI crashed: ${t.failure}")
        Actor.stopped
    }

    Actor.immutable { (c, msg) =>
      msg match {
        case Right(StartGame(owner, player, _)) =>
          val ref = c.spawn(aiInstance.behavior(GameHandler.initialGameState(owner, player, gameConfig)), s"ai-game:${owner.id}vs${player.id}")
          wrapping(ref)
        case Right(r) =>
          logger.error(s"Illegal message for not yet initialised ai: $r")
          Actor.stopped
        case Left(l) =>
          logger.error(s"Illegal message for not yet initialised ai: $l")
          Actor.stopped
      }
    }
  }


}

object AiAdapter {
  type AIAdapterMessage = Either[GameResponse, AiCommand]
  sealed trait AiCommand
  case class StartGame(owner: OwnerId, joining: OwnerId, ref: UnsafeActorRef[GameResponse]) extends AiCommand
  case class Kill(id: OwnerId) extends AiCommand

}

