package services.game


import akka.typed._
import akka.typed.scaladsl.{Actor, ActorContext}
import com.example.config.GameConfig
import com.example.model.GameResponse._
import com.example.model._
import com.example.model.units.UnitInstance
import models.IdentifiedCommand
import services.game.GameHandler._
import services.game.GameRepository.{GameType, NotUsed}

import scala.collection.SortedSet
import scala.concurrent.duration._
import scala.language.implicitConversions
class GameHandler(commandHandler: PlayerCommandHandler, config: GameConfig) extends UnitHandler {
  private val logger = play.Logger.of(classOf[GameHandler])

  private def startGame: Behavior[GameEvent] = Actor.immutable { (_, msg) =>
    msg match {
      case CreateEvent(key, _,  ref) =>
        val state = OnePlayerJoined(key)
        initializeGame(ref, state)
      case c =>
        logger.error(s"Invalid state of game handler: Game not started but received: $c")
        Actor.stopped
    }
  }

  private def initializeGame(firstRef: UnsafeActorRef[GameResponse], halfInitialized: OnePlayerJoined): Behavior[GameEvent] =
    Actor.immutable { (ctx, msg) =>
      msg match {
        case JoinEvent(_, playerId, secondRef) =>
          val initialState = initialGameState(halfInitialized.player, playerId, gameConfig)
          ctx.schedule(15.millis, ctx.self, NextEpoch)
          handleGameFlow(initialState, initialState)(firstRef, secondRef)
        case LeaveEvent(_) =>
          Actor.stopped
        case c =>
          logger.error(s"Game created but not started, but received: $c")
          Actor.stopped
      }
    }


  private def handleGameFlow(lastState: Ongoing, gameState: Ongoing)(fr: UnsafeActorRef[GameResponse], sr: UnsafeActorRef[GameResponse]): Behavior[GameEvent] = {
        Actor.immutable { (ctx, msg) =>
          msg match {
            case CommandEvent(IdentifiedCommand(_, playerId, cmd)) =>
              if (playerId == gameState.player1) {
                val s = commandHandler.handleCommand(gameState.player1, gameState.player1State, cmd)(fr)
                handleGameFlow(lastState, gameState.copy(player1State = s))(fr, sr)
              } else if (playerId == gameState.player2) {
                val s = commandHandler.handleCommand(gameState.player2, gameState.player2State, cmd)(sr)
                handleGameFlow(lastState, gameState.copy(player2State = s))(fr, sr)
              } else {
                Actor.stopped
              }
            case NextEpoch =>
              handleEpoch(lastState, addIncome(gameState), ctx)(fr, sr)
            case m =>
              logger.warn(s"Game ended due to: $m")
              fr ! GameEnded
              sr ! GameEnded
              Actor.stopped
          }
        }

      }

  private def addIncome(state: Ongoing): Ongoing = {
    val p1Units = state.units.valuesIterator.flatten.filter(_.ownerId == state.player1)
    val p1GoldMultiplier = p1Units.map(i => BigDecimal(i.x) / config.width * 2).sum / (p1Units.size + 1) * math.log(p1Units.size + 2)
    val p2Units = state.units.valuesIterator.flatten.filter(_.ownerId == state.player2)
    val p2GoldMultiplier = p2Units.map(i => BigDecimal(config.width - i.x) / config.width * 2).sum / (p1Units.size + 1) * math.log(p1Units.size + 2)
    state.copy(
      player1State = state.player1State.withGold(_ + (p1GoldMultiplier * gameConfig.goldIncome).intValue()),
      player2State = state.player2State.withGold(_ + (p2GoldMultiplier * gameConfig.goldIncome).intValue())
    )
  }

  private def handleOnePlayerDeath(lastState: Ongoing, winner: OwnerId)(fr: UnsafeActorRef[GameResponse], sr: UnsafeActorRef[GameResponse]): Unit = {
    logger.warn(s"Game ended with victory of player: $winner")
    fr ! GameEnded
    sr ! GameEnded
  }

  private def handleEpoch(lastState: Ongoing, gameState: Ongoing, context: ActorContext[GameEvent])(fr: UnsafeActorRef[GameResponse], sr: UnsafeActorRef[GameResponse]): Behavior[GameEvent] = {
    val withoutDead = removeDead(gameState)
    val (stateAfterTick, _) = tickForUnits(withoutDead)
    val unitsToBroadcast = (for((_, m) <- stateAfterTick.units ; u <- m) yield u).to[MSeq]
    val msgToSend = UnitResponse(unitsToBroadcast)
    fr ! msgToSend
    sr ! mirrorUnitsForSecondPlayer(msgToSend)
    fr ! PlayerStateResponse(stateAfterTick.player1, stateAfterTick.player1State diff lastState.player1State, stateAfterTick.player2State.baseHealth)
    sr ! PlayerStateResponse(stateAfterTick.player2, stateAfterTick.player2State diff lastState.player2State, stateAfterTick.player1State.baseHealth)
    context.schedule(15.millis, context.self, NextEpoch)
    if (stateAfterTick.player1State.baseHealth <= 0) {
      handleOnePlayerDeath(stateAfterTick, stateAfterTick.player2)(fr, sr)
      Actor.stopped
    } else if (stateAfterTick.player2State.baseHealth <= 0) {
      handleOnePlayerDeath(stateAfterTick, stateAfterTick.player1)(fr, sr)
      Actor.stopped
    } else {
      handleGameFlow(stateAfterTick, stateAfterTick)(fr, sr)
    }
  }

  private def mirrorUnitsForSecondPlayer(resp: UnitResponse): UnitResponse = UnitResponse(
    resp.instances.map(i => i.copy(x = config.width - i.x))
  )

  def behavior: Behavior[GameEvent] = startGame

  override val gameConfig: GameConfig = config

  override protected def startPosition(id: OwnerId, state: Ongoing): Int = if (id == state.player1) {
    gameConfig.spawnOffset
  } else {
    gameConfig.width - gameConfig.spawnOffset
  }
}

object GameHandler {

  def initialGameState(p1: OwnerId, p2: OwnerId, config: GameConfig) = Ongoing(
    player1 = p1,
    player2 = p2,
    player1State = PlayerState.fromGameConfig(config),
    player2State = PlayerState.fromGameConfig(config),
    (0 until config.laneNumber).map(_ -> SortedSet.empty[UnitInstance]).toMap
  )


  sealed trait GameEvent
  final case class CreateEvent(id: OwnerId, gameType: GameType = NotUsed, ref: UnsafeActorRef[GameResponse]) extends GameEvent
  final case class JoinEvent(id: OwnerId, playerId: OwnerId, ref: UnsafeActorRef[GameResponse]) extends GameEvent
  final case class LeaveEvent(id: OwnerId) extends GameEvent
  final case class CommandEvent(command: IdentifiedCommand) extends GameEvent
  final case object NextEpoch extends GameEvent

  //lifting command to event
  implicit def commandToEvent(identifiedCommand: IdentifiedCommand): CommandExtension = new CommandExtension(identifiedCommand)

  class CommandExtension(private val command: IdentifiedCommand) extends AnyVal {
    def toEvent: GameEvent = CommandEvent(command)
  }

}
