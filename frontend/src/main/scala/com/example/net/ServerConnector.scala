package com.example.net

import java.nio.ByteBuffer
import java.util.concurrent.TimeoutException

import boopickle.Default._
import com.example.model._
import ServerConnector._
import com.example.model.GameResponse._
import com.example.util._
import monix.execution.Ack.Continue
import monix.execution.Cancelable
import monix.reactive._
import org.scalajs.dom.raw.WebSocket
import org.scalajs.dom.window

import scala.concurrent.{Future, Promise}
import scala.concurrent.duration._
import scala.collection.mutable.{Map => MMap}
import scala.scalajs.js
import scala.scalajs.js.timers._
import scala.scalajs.js.typedarray.{ArrayBuffer, TypedArrayBuffer}
import monix.execution.Scheduler.Implicits.global
import monix.reactive.observables.ConnectableObservable

import js.Dynamic.{global => g}
import scala.collection.immutable.Queue

trait ServerConnector extends Connector with Configuration {
  private val webSocket = webSocketConnection
  private val awaitingCommands: MMap[String, Queue[Promise[CommandResponse]]] =
    js.Dictionary[Queue[Promise[CommandResponse]]]().withDefaultValue(Queue.empty)

  private lazy val genericStream: ConnectableObservable[GameResponse] = {
    Observable.create[GameResponse](OverflowStrategy.Unbounded) { sync =>
      webSocket.onmessage = ev => {
        val bytes = TypedArrayBuffer.wrap(ev.data.asInstanceOf[ArrayBuffer])
        val dematerialized = Unpickle[GameResponse].fromBytes(bytes)
        sync.onNext(dematerialized)
      }
      webSocket.onerror = ev => sync.onError(new Exception(ev.message))
      Cancelable { () =>
        sync.onComplete()
        webSocket.close()
      }
    }.publish
  }


  private def executeCommandResponseStream() = genericStream.collect {
    case cr: CommandResponse => cr
  }.subscribe(cr => cr match {
    case CommandResponse(command, _) =>
      val q = awaitingCommands(command.toString)
      if (q.headOption.exists(_.trySuccess(cr))) {
        val (_, d) = q.dequeue
        awaitingCommands(command.toString) = d
      }
      Continue
  })


  override val playerStateStream: Observable[PlayerStateResponse] = {
    genericStream.collect {
      case psr: PlayerStateResponse => psr
    }
  }

  override val unitStream: Observable[UnitResponse] = {
    genericStream.collect {
      case i: UnitResponse => i
    }
  }

  override def request(input: Command): Future[CommandResponse] = {
    val serialized = Pickle.intoBytes(input)
    val arrayBuffer = bbToArrayBuffer(serialized)
    webSocket.send(arrayBuffer)
    val promise = Promise[CommandResponse]()
    awaitingCommands(input.toString) = awaitingCommands(input.toString).enqueue(promise)
    setTimeout(1.second) {
      if (promise.tryFailure(new TimeoutException("Failed to receive response from server"))) {
        val (_, d) = awaitingCommands(input.toString).dequeue
        awaitingCommands(input.toString) = d
      }
    }
    promise.future
  }

  override def endGameResponse(): Future[Unit] = {
    val optFuture = genericStream.collect {
      case GameEnded => ()
    }.runAsyncGetFirst
    for (opt <- optFuture if opt.isDefined) yield ()
  }

  override def connect(): Unit = {
    executeCommandResponseStream()
    genericStream.connect()
  }
}

object ServerConnector {

  trait Configuration { self: Connector =>
    def protocol: String = if (window.location.protocol == "https:") "wss" else "ws"

    def wsAddress: String = {
      val call: PlayCall = g.jsRoutes.controllers.GameController.gameChannel(playerID.id, gameID.id)
      val tmp = call.webSocketURL
      println(tmp)
      tmp
    }

    def webSocketConnection: WebSocket = {
      val connection = new WebSocket(wsAddress)
      connection.binaryType = "arraybuffer"
      connection
    }
  }

  def bbToArrayBuffer(buffer: ByteBuffer): ArrayBuffer = {
    val arrayBytes = bbToArrayBytes(buffer)
    val arrayBuf = new ArrayBuffer(arrayBytes.length)

    val typedAB = TypedArrayBuffer.wrap(arrayBuf)
    typedAB.put(arrayBytes)

    arrayBuf
  }

  def jsArrayToByteBuffer(bytes: js.Array[Byte]): ByteBuffer = {
    ByteBuffer.wrap(bytes.toArray)
  }
}
