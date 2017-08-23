package com.example

import boopickle.Default._
import com.example.components.BasicComponents
import com.example.model._
import com.example.styles.JsButton
import com.example.util._
import monix.execution.Ack._
import monix.execution.Cancelable
import monix.execution.Scheduler.Implicits.global
import monix.reactive._
import org.scalajs.dom.raw.WebSocket
import org.scalajs.dom.{document, window}

import scala.scalajs.js.annotation._
import scala.scalajs.js.typedarray._
import scalatags.JsDom.all._
import scalacss.ScalatagsCss._
import scala.scalajs.js.Dynamic.{global => g}

class GameLobby(wsQuery: String, joinGameUrl: String, resetAppUrl: String) {

  import com.example.net.ServerConnector._

  private val webSocket: WebSocket = {
    println(wsQuery)
    val tmpSocket = new WebSocket(wsQuery)
    tmpSocket.binaryType = "arraybuffer"
    tmpSocket
  }

  private[this] val cancelButton = div(JsButton.basicButton)(onclick := { () => cancelGame()}) {
    "Cancel"
  }

  document.getElementById(BasicComponents.lobbyArea).appendChild(cancelButton.render)

  private val lobbyObservable: Observable[LobbyMessage] = Observable.create(OverflowStrategy.Unbounded) { sync =>
    webSocket.onmessage = event => {
      val bytes = TypedArrayBuffer.wrap(event.data.asInstanceOf[ArrayBuffer])
      val dematerialized = Unpickle[LobbyMessage].fromBytes(bytes)
      sync.onNext(dematerialized)
    }
    webSocket.onerror = err => sync.onError(new Exception(err.message))
    Cancelable { () =>
      sync.onComplete()
      webSocket.close()
    }
  }

  private def cancelGame(): Unit = {
    val bytes = Pickle.intoBytes(LobbyCommand.Cancel)
    val bb = bbToArrayBuffer(bytes)
    println("Canceling game!")
    webSocket.send(bb)
    window.location.href = resetAppUrl
  }

  def start(): Cancelable = lobbyObservable.subscribe { msg =>
    msg match {
      case LobbyMessage.Started =>
        redirectToGame()
        Stop
      case LobbyMessage.Canceled =>
        Stop
    }
  }

  private def redirectToGame(): Unit = {
    val closingLobby = Pickle.intoBytes(LobbyCommand.Close(true))
    val bytes = bbToArrayBuffer(closingLobby)
    webSocket.send(bytes)
    println(joinGameUrl)
    window.location.href = joinGameUrl
  }

}

@JSExportTopLevel("GameLobby")
object GameLobby {
  @JSExport
  def main(gameId: Int): Unit = {
    val r = g.jsRoutes.controllers.GameController
    val wsCall: PlayCall = r.gameLobby(gameId)
    val succesfulRedirectCall: PlayCall = r.joinGame(gameId)
    val cancelRedirectCall: PlayCall = r.reset()
    val wsString = wsCall.webSocketURL()
    val gameJoinString = succesfulRedirectCall.url
    val gameResetString = cancelRedirectCall.url
    println(wsString)
    println(gameJoinString)
    println(gameResetString)
    new GameLobby(wsString, gameJoinString, gameResetString).start()
  }
}
