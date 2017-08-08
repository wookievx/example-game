package com.example

import java.nio.ByteBuffer

import boopickle.Default._
import com.example.components.BasicComponents
import com.example.config.Entry
import com.example.core.MainFrame
import com.example.model._
import com.example.model.Command._
import com.example.model.GameResponse._
import SpecificResponse._
import com.example.model.units.UnitObject
import com.example.net.ServerConnector
import com.example.ui.UiDefinitions
import com.example.ui.draw.ImageModule
import monix.execution.Cancelable
import org.scalajs.dom
import org.scalajs.dom.html.Canvas

import scala.collection.mutable.{Seq => MSeq}
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import scala.scalajs.js.timers.SetIntervalHandle
import scala.util.{Failure, Success}
import scalatags.JsDom.all._
import scala.scalajs.js.typedarray._

abstract class WarGame(availableUnits: MSeq[UnitObject],
                       val imageModule: ImageModule,
                       val clientWidth: Int,
                       val clientHeight: Int,
                       val realWidth: Int,
                       val realHeight: Int,
                       val playerID: OwnerId,
                       val gameID: OwnerId,
                       val initialState: PlayerState) extends MainFrame with ServerConnector {

  lazy val mainCanvas: Canvas = {
    val c = canvas().render
    c.height = clientHeight
    c.width = clientWidth
    c.style.height = s"${clientHeight}px"
    c.style.width = s"${clientWidth}px"
    c
  }

  private val tooltip = UiDefinitions.createTooltip(availableUnits, imageModule, playerID, createUnit)

  private val playerInfoId = "p-info-id"
  private val opponentInfoId = "o-info-id"

  private def resourcesText(who: String, gold: String, health: Int) = s"$who resources: gold: $gold, health: $health"

  private val gameInfo = span(h3(id := playerInfoId)(resourcesText("Your", "0", 0)), h3(id := opponentInfoId)(resourcesText("Enemy", "???", 0)))

  override def updateStatistics(gold: Int, health: Int): Unit = {
    dom.document.getElementById(playerInfoId).textContent = resourcesText("Your", String.valueOf(gold), health)
  }


  override def updateOpponentStatistics(health: Int): Unit = {
    dom.document.getElementById(opponentInfoId).textContent = resourcesText("Enemy", "???", health)
  }

  def initialize(): Unit = {
    dom.document.getElementById(BasicComponents.gameArea).appendChild(gameInfo.render)
    dom.document.getElementById(BasicComponents.gameArea).appendChild(mainCanvas)
    dom.document.getElementById(BasicComponents.gameArea).appendChild(tooltip.render)
    start()
  }


  private def showErrorPopup(response: SpecificResponse): Future[Unit] = Future.successful {
    dom.window.alert(s"Game error: $response")
  }

  private def createUnit(selectedUnit: UnitObject): Unit = {

    def requestUnitCreation(response: SpecificResponse): Future[SpecificResponse] = response match {
      case UnitCreationPossible =>
          request(CreateUnit(selectedUnit)).map { case CommandResponse(_, sr) => sr }
      case r =>
        showErrorPopup(r).map(_ => r)
    }

    val result = for {
      CommandResponse(_, canCreateResponse) <- request(CanCreateUnit(selectedUnit))
      creationResponse <- requestUnitCreation(canCreateResponse)
    } yield creationResponse

    result.onComplete {
      case Success(LaneAssigned(n)) => setCurrentLane(n)
      case Success(response) => showErrorPopup(response)
      case Failure(error) => error.printStackTrace()
    }
  }

  override def endGame(): Unit = {
    dom.window.alert("Game ended!")
  }
}

@JSExportTopLevel("WarGame")
object WarGame {

  @js.native
  trait JsEntry extends js.Object {
    val key: String
    val value: String
  }

  @js.native
  trait JsOwnerId extends js.Object {
    val id: Int
  }

  @js.native
  trait JsPlayerState extends js.Object {
    val baseHealth: Int
    val gold: Int
  }

  implicit class JsEntryExtension(private val jsEntry: JsEntry) extends AnyVal {
    def toSharedEntry: Entry[String] = Entry(jsEntry.key, jsEntry.value)
  }

  implicit class JsIdExtension(private val jsOwnerId: JsOwnerId) extends AnyVal {
    def toOwnerId: OwnerId = OwnerId(jsOwnerId.id)
  }


  @JSExport
  def main(playerId: JsOwnerId, gameOwner: JsOwnerId, width: Int, lanes: Int, state: JsPlayerState,
           units: js.Array[js.Array[Byte]], icons: js.Array[JsEntry], symbols: js.Array[JsEntry]): Unit = {
    val unitIcons = icons.map(_.toSharedEntry)
    val unitSymbols = symbols.map(_.toSharedEntry)
    val availableUnits = units.map(ServerConnector.jsArrayToByteBuffer).map(Unpickle[UnitObject].fromBytes)
    val imageModule = ImageModule.fromArguments(unitIcons.toArray, unitSymbols.toArray)

    val game = new WarGame(availableUnits,
      imageModule,
      (dom.window.innerWidth / 2).toInt,
      (dom.window.innerHeight / 2).toInt,
      width, lanes, playerId.toOwnerId, gameOwner.toOwnerId, PlayerState(baseHealth = state.baseHealth, gold = state.gold)) {
      override def method: String = "channel"
    }
    game.initialize()
  }

}
