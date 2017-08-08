package services.game

import akka.typed._
import akka.typed.scaladsl._
import com.example.config.GameConfig
import com.example.model._
import models.IdentifiedCommand
import GameHandler._
import GameRepository._
import scala.language.implicitConversions

class GameRepository(gameConfig: GameConfig) {

  private val logger = play.api.Logger(getClass)
  lazy val system = ActorSystem("game-repository", behavior(Map.empty))

  def behavior(games: Map[OwnerId, GameInstance]): Behavior[GameMessage] = {
    Actor.immutable { (ctx, msg) =>
      msg match {
        case Left(OpenGamesRequest(replyTo)) =>
          replyTo ! games.values
          Actor.same
        case Right(e@CreateEvent(id, gameType, _)) =>
          val updatedGames = games.updated(id, {
            lazy val ch = new PlayerCommandHandler(gameConfig, gameConfig.instanceIdFactory)
            lazy val gh = new GameHandler(ch, gameConfig)
            val r = ctx.spawn(gh.behavior, s"${classOf[GameHandler].getSimpleName}-${games.size}")
            GameInstance(id, gameType, r)
          })
          updatedGames(id).ref ! e
          behavior(updatedGames)
        case Right(e@JoinEvent(ownerId, _, _)) =>
          val nextRef = games(ownerId)
          nextRef ! e
          Actor.same
        case Right(e@LeaveEvent(id)) =>
          games.get(id).foreach(_ ! e)
          behavior(games - id)
        case Right(CommandEvent(c@IdentifiedCommand(ownerId, _, _))) =>
          games.get(ownerId).foreach(_ ! CommandEvent(c))
          Actor.same
        case Right(c) =>
          logger.error(s"Illegal command: $c")
          Actor.same
      }

    }
  }

}

object GameRepository {



  type GameMessage = Either[OpenGamesRequest, GameEvent]

  //lifting implicit conversions
  implicit def toGameMessage(event: GameEvent): GameMessage = event.toRight
  implicit def toGameMessage(request: OpenGamesRequest): GameMessage = request.toLeft

  case class OpenGamesRequest(respondTo: ActorRef[Iterable[GameInstance]])

  case class GameInstance(id: OwnerId, gameType: GameType, ref: ActorRef[GameEvent]) {
    @inline def tell(msg: GameEvent): Unit = ref tell msg
    @inline def !(msg: GameEvent): Unit = tell(msg)
  }

  sealed trait GameType {
    def name: String
  }
  case class Normal(name: String) extends GameType
  case class AI(name: String) extends GameType
  case object NotUsed extends GameType { val name = "" }

}
