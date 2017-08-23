package services.game

import akka.typed._
import akka.typed.scaladsl._
import com.example.config.GameConfig
import com.example.model._
import models.IdentifiedCommand
import GameHandler._
import GameRepository._
import com.example.model.GameResponse.GameEnded

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
        case Right(CreateEvent(id, gameType, ref)) =>
          val updatedGames = games.updated(id, {
            lazy val ch = new PlayerCommandHandler(gameConfig, gameConfig.instanceIdFactory)
            lazy val gh = new GameHandler(ch, gameConfig)
            val r = ctx.spawn(gh.behavior(id, ref), s"${classOf[GameHandler].getSimpleName}-${id.id}")
            GameInstance(id, gameType, r)
          })
          behavior(updatedGames)
        case Right(e@JoinEvent(ownerId, _, ref)) =>
          val nextRef = games.get(ownerId)
          nextRef.foreach(_ ! e)
          if (nextRef.isEmpty) {
            ref ! GameEnded
          }
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
