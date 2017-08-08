package controllers

import java.nio.ByteBuffer

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.OverflowStrategy
import akka.stream.scaladsl._
import akka.typed.ActorRef
import akka.typed.scaladsl.AskPattern._
import akka.util.Timeout
import boopickle.Default._
import com.example.config.GameConfig
import com.example.model._
import com.example.model.units.UnitTraits
import com.example.styles.Tooltip
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

  def facadeJs(id: OwnerId, gameId: OwnerId) = Action { implicit request =>
    val au = config.availableUnits.toList
    val units: List[UnitTraits] = au
    Ok(views.js.script.facade.render(id, gameId, config.width, config.laneNumber, units, PlayerState.fromGameConfig(config), au, assetResolver))
  }

  def tooltipStyles = Action { implicit request =>
    import com.example.styles.CssSettings._
    Ok(Tooltip.render[String]).as(CSS)
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

  def joinGame(gameId: OwnerId) = Action { implicit request =>
    val pid = config.ownerIdFactory.next()
    val name = request.session.get("nick").getOrElse(String.valueOf(pid.id))
    nameService.save(pid, name)
    Ok(views.html.init("Game", assetResolver, pid, gameId, translateIdToName))
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
          gameHandler ! JoinEvent(ownerId, playerId, ref).toRight
        }

      val in = Sink.actorRef(unsafeSource, LeaveEvent(ownerId))

      Flow.fromSinkAndSource(in, out)
    }


    deserializeState.via(gameFlow).via(serializeState)
  }


  private case class GameHandlerWrapper(gameHandler: GameHandler)

}
