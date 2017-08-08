package com.example.core


import com.example.config.Entry
import com.example.model.Command.NextLane
import com.example.model.GameResponse._
import com.example.model.{OwnerId, PlayerState}
import com.example.model.SpecificResponse._
import com.example.model.units.UnitInstance
import com.example.model.units.UnitState._
import com.example.net.Connector
import com.example.ui.draw._
import monix.execution.Ack.Continue
import monix.execution.Scheduler.Implicits.global
import org.scalajs.dom.html.Canvas

import scala.collection.mutable.{Map => MMap}
import scala.concurrent.duration._
import scala.scalajs.js
import scala.scalajs.js.timers._
import scala.util.Success

trait MainFrame extends Connector with FiniteRepeater {

  private val watchedInstances: MMap[String, UnitInstance] = js.Dictionary[UnitInstance]()

  protected def start(): Unit = {
    setInterval(200.millis) {
      request(NextLane).onComplete {
        case Success(CommandResponse(_, LaneAssigned(n))) =>
          currentLane = n
        case _ =>
      }
    }
    loop(playerID)

    //actions synchronised with server (update health, gold, production queue)
    unitStream.subscribe { gameResponse =>
      val instances = gameResponse.instances
      watchedInstances.transform { case (_, i) => i.copy(unitState = i.unitState.toMoving)}
      watchedInstances ++= instances.iterator.map(i => i.instanceID.toStringId -> i)
      watchedInstances.retain((_, i) => i.unitState != Dead)
      Continue
    }
    val publishedStateStream = playerStateStream.publish
    val selfStream = publishedStateStream.collect { case PlayerStateResponse(_, s, _) => s}
    val opponentStream = publishedStateStream.collect { case PlayerStateResponse(_, _, g) => g}
    selfStream.scan(initialState)(_ withDiff _).subscribe { ps =>
      updateStatistics(ps.gold, ps.baseHealth)
      Continue
    }
    opponentStream.subscribe { gold =>
      updateOpponentStatistics(gold)
      Continue
    }

    endGameResponse().onComplete(_ => endGame())
    publishedStateStream.connect()
    connect()
  }



  def mainCanvas: Canvas
  def clientWidth: Int
  def clientHeight: Int
  def realWidth: Int
  def realHeight: Int
  def imageModule: ImageModule
  def endGame(): Unit
  def updateStatistics(gold: Int, health: Int)
  def updateOpponentStatistics(health: Int)
  def initialState: PlayerState


  private val gameCanvas = GameCanvas.defaultCanvas(imageModule, mainCanvas, clientHeight, clientWidth, realWidth, realHeight, playerID)

  private var currentLane: Int = 0

  def setCurrentLane(n: Int): Unit = currentLane = n

  def loop(selfId: OwnerId): SetIntervalHandle = setInterval(30.millis) {
    import js.JSConverters._
    moveUnits()
    gameCanvas.drawGameState(watchedInstances.values.toJSArray)
    drawHealthBars(watchedInstances.collect { case (id, UnitInstance(_, _, _, _, _, Attacked(_, _, _) | Attacking(_, _, _))) => id })
    executeEpoch()
    gameCanvas.showLaneInfo(currentLane)
  }

  def moveUnits(): Unit = watchedInstances.transform {
    case (_, i@UnitInstance(_, id, uo, x, _, Moving(_, _))) if id == this.playerID =>
      i.copy(x = x + uo.movementSpeed)
    case (_, i@UnitInstance(_, _, uo, x, _, Moving(_, _))) =>
      i.copy(x = x - uo.movementSpeed)
    case (_, i) => i
  }

  private def drawHealthBars(attackedUnits: Iterable[String]): Unit = {
    orderExecution(60, () => gameCanvas.drawHealthBarsFor(attackedUnits.flatMap(watchedInstances.get)))
  }


}

object MainFrame {

  @js.native
  trait JsEntry extends js.Object {
    val key: String
    val value: String
  }

  implicit class JsEntryExtension(private val jsEntry: JsEntry) extends AnyVal {
    def toSharedEntry: Entry[String] = Entry(jsEntry.key, jsEntry.value)
  }

}
