package controllers

import java.nio.ByteBuffer

import akka.NotUsed
import akka.actor.{Actor, ActorSystem, Props}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl._
import akka.typed.ActorRef
import akka.typed.scaladsl.AskPattern._
import akka.util.Timeout
import boopickle.Default._
import com.example.config.GameConfig
import com.example.model.LobbyCommand.{Cancel, Close}
import com.example.model._
import com.example.model.units.UnitTraits
import com.example.styles.{JsButton, Tooltip}
import models.IdentifiedCommand
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.AssetResolver
import services.game.GameHandler._
import services.game.GameRepository._
import services.game._


class GameController(cc: ControllerComponents,
                     assetResolver: AssetResolver,
                     actorSystem: ActorSystem,
                     gameServer: GameServer,
                     nameService: NameService,
                     config: GameConfig,
                     timeout: Timeout) extends AbstractController(cc) with I18nSupport {
  private implicit val sys = actorSystem

  implicit private[this] val (s1, e1, t1) = (actorSystem.scheduler, actorSystem.dispatcher, timeout)
  private val logger = play.api.Logger(getClass)

  def facadeJs(id: OwnerId, gameId: OwnerId) = Action { implicit request =>
    val au = config.availableUnits.toList
    val units: List[UnitTraits] = au
    Ok(views.js.script.facade.render(id, gameId, config.width, config.laneNumber, units, PlayerState.fromGameConfig(config), au, assetResolver))
  }

  def lobbyFacade(gameId: OwnerId) = Action { implicit request =>
    Ok(views.js.script.lobbyFacade.render(gameId.id))
  }

  def tooltipStyles = Action { implicit request =>
    import com.example.styles.CssSettings._
    Ok(Tooltip.render[String]).as(CSS)
  }

  def jsButtonStyles = Action { implicit request =>
    import com.example.styles.CssSettings._
    Ok(JsButton.render[String]).as(CSS)
  }

  def openGames: Action[AnyContent] = Action.async { implicit request =>
    val future = gameServer.actorRef ? (OpenGamesRequest(_: ActorRef[Iterable[GameInstance]]))
    future.map(u => Ok(views.html.games("Open games", u)))
  }

  private def translateIdToName(id: OwnerId): String = {
    nameService.get(id).getOrElse(s"Unknown id: $id")
  }

  def gameChannel(playerId: Int, ownerId: Int): WebSocket = WebSocket.accept[Array[Byte], Array[Byte]] { _ =>
    joinFlow(OwnerId(playerId), OwnerId(ownerId))
  }

  def joinGame(gameId: OwnerId) = Action.async { implicit request =>
    val pid = config.ownerIdFactory.next()
    val name = request.session.get("nick").getOrElse(String.valueOf(pid.id))
    nameService.save(pid, name)
    val future = gameServer.actorRef ? (OpenGamesRequest(_: ActorRef[Iterable[GameInstance]]))
    future.collect {
      case elems if elems.exists(_.id == gameId) =>
        Ok(views.html.init("Game", assetResolver, pid, gameId, translateIdToName))
      case _ =>
        BadRequest("The game you have chosen does not exist!")
    }
  }


  def createGame = Action { implicit request =>
    val pid = config.ownerIdFactory.next()
    val name = request.session.get("nick").map(nick => s"$nick-game").getOrElse(String.valueOf(pid.id))
    nameService.save(pid, name)
    Ok(views.html.lobby("lobby", pid, pid, nameService.get(_).getOrElse(String.valueOf(pid.id))))
  }

  def gameLobby(gameId: OwnerId): WebSocket = WebSocket.accept[Array[Byte], Array[Byte]] { _ =>
    lobbyFlow(gameId, nameService.get(gameId).getOrElse(s"game-${gameId.id}"))
  }

  def reset = Action { implicit request =>
    Redirect(routes.GreeterController.index()).withNewSession
  }

  private def lobbyFlow(gameId: OwnerId, gameName: String): Flow[Array[Byte], Array[Byte], NotUsed] = {
    val deserialize: Flow[Array[Byte], LobbyCommand, NotUsed] = Flow.fromFunction { bytes =>
      Unpickle[LobbyCommand].fromBytes(ByteBuffer.wrap(bytes))
    }
    val serializeState: Flow[LobbyMessage, Array[Byte], NotUsed] = {
      Flow.fromFunction(r => bbToArrayBytes(Pickle.intoBytes(r)))
    }
    val lobbyFlow: Flow[LobbyCommand, LobbyMessage, NotUsed] = {
      val gameHandler = gameServer.actorRef

      val emptyBehavior: PartialFunction[Any, Unit] = {
        case _ =>
      }

      val mappingRef = actorSystem.actorOf(Props(new Actor {
        override def receive: Receive = {
          case Cancel =>
            gameHandler ! LeaveEvent(gameId)
            context.become(emptyBehavior)
          case Close(false) =>
            gameHandler ! LeaveEvent(gameId)
            context.become(emptyBehavior)
          case Close(true) =>
            context.become(emptyBehavior)
        }
      }))
      val out = Source.actorRef[LobbyMessage](100000, OverflowStrategy.dropNew)
        .mapMaterializedValue { ref => gameHandler ! CreateEvent(id = gameId, gameType = Normal(gameName), ref = ref) }
      val in = Sink.actorRef(mappingRef, Close(false))
      Flow.fromSinkAndSource(in, out)
    }

    deserialize.via(lobbyFlow).via(serializeState)
  }

  private def joinFlow(playerId: OwnerId, ownerId: OwnerId): Flow[Array[Byte], Array[Byte], NotUsed] = {

    val deserializeState: Flow[Array[Byte], GameEvent, NotUsed] = Flow.fromFunction { bytes =>
        val command = Unpickle[Command].fromBytes(ByteBuffer.wrap(bytes))
        CommandEvent(IdentifiedCommand(ownerId, playerId, command))
      }

    val serializeState: Flow[GameResponse, Array[Byte], NotUsed] =
      Flow.fromFunction(r => bbToArrayBytes(Pickle.intoBytes(r)))


    val gameFlow: Flow[GameEvent, GameResponse, NotUsed] = {
      val gameHandler = gameServer.actorRef
      val unsafeSource = gameServer.unsafeSource
      val out = Source.actorRef[GameResponse](100000, OverflowStrategy.dropNew)
        .mapMaterializedValue { ref =>
          logger.warn(s"Player: ${nameService.get(playerId).getOrElse(String.valueOf(playerId))} joined")
          gameHandler ! JoinEvent(ownerId, playerId, ref).toRight
        }

      val in = Sink.actorRef(unsafeSource, LeaveEvent(ownerId))

      Flow.fromSinkAndSource(in, out)
    }


    deserializeState.via(gameFlow).via(serializeState)
  }


  private case class GameHandlerWrapper(gameHandler: GameHandler)

}
