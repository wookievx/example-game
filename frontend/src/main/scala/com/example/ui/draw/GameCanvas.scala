package com.example.ui.draw

import GameCanvas.ParametersConfig
import com.example.model.OwnerId
import com.example.model.units.{UnitInstance, UnitObject, UnitTraits}
import org.scalajs.dom
import org.scalajs.dom.html.{Canvas, Image}
import org.scalajs.dom.raw.CanvasRenderingContext2D

import scala.collection.mutable.{Map => MMap}
import scala.scalajs.js


abstract class GameCanvas(imageModule: ImageModule,
                          canvas: Canvas,
                          cheight: Int,
                          cwidth: Int,
                          width: Int,
                          laneNumber: Int,
                          selfId: OwnerId) { self: ParametersConfig =>

  private val unitSymbols = MMap[UnitTraits, Image]()

  private def unitSymbol(unitTraits: UnitTraits): Option[Image] = {
    unitSymbols.get(unitTraits).orElse {
      val res = imageModule.unitGameSymbolDom(unitTraits).map(_.render)
      res.foreach(unitSymbols.put(unitTraits, _))
      res
    }
  }

  private val xAdj = cwidth.toDouble / width
  private val lineHeight = cheight.toDouble / laneNumber
  private def drawX(x: Int) = x * xAdj
  private def drawY(y: Int) = y * lineHeight

  private val ctx = canvas.getContext("2d").asInstanceOf[CanvasRenderingContext2D]

  private def drawBackground(): Unit = {
    ctx.fillStyle = "green"
    ctx.fillRect(0, 0, ctx.canvas.width, ctx.canvas.height)

    for (i <- 1 until laneNumber) {
      ctx.beginPath()
      ctx.setLineDash(js.Array(10, 10))
      ctx.moveTo(0, i * lineHeight)
      ctx.lineTo(cwidth, i * lineHeight)
      ctx.stroke()
    }
  }

  def showLaneInfo(lane: Int): Unit = {
    ctx.strokeStyle = "red"
    ctx.beginPath()
    ctx.moveTo(0, lane * lineHeight)
    ctx.lineTo(unitWidth.map(_.toDouble).getOrElse(40), lane * lineHeight + lineHeight / 2)
    ctx.lineTo(0, (lane + 1) * lineHeight)
    ctx.lineWidth = 5
    ctx.stroke()
  }

  def drawGameState(units: js.Array[UnitInstance]): Unit = {
    units.sort(_.y - _.y)
    drawBackground()
    for ((UnitInstance(_, id, uo, x, y, _), i) <- units.zipWithIndex) {
      unitSymbol(uo) match {
        case Some(img) =>
          val width = unitWidth.getOrElse(img.width)
          val height = unitHeight.getOrElse(img.height)
          if (id == selfId) {
            ctx.drawImage(img, drawX(x) - width / 2, drawY(y), width, height)
          } else {
            ctx.save()
            ctx.translate(drawX(x) + width / 2, drawY(y) + height / 2)
            ctx.scale(-1, 1)
            ctx.drawImage(img, 0, -height / 2, width, height)
            ctx.restore()
          }
        case None =>
          if (id == selfId) {
            ctx.fillStyle = "blue"
            ctx.beginPath()
            ctx.arc(drawX(x), drawY(y) + (i - units.size / 2) * dotRadius, dotRadius, 0, 6.28)
            ctx.closePath()
            ctx.fill()
          } else {
            ctx.fillStyle = "red"
            ctx.beginPath()
            ctx.arc(drawX(x), drawY(y) + (i - units.size / 2) * dotRadius, dotRadius, 0, 6.28)
            ctx.closePath()
            ctx.fill()
          }
      }

    }
  }

  def drawHealthBarsFor(units: Iterable[UnitInstance]): Unit = {
    ctx.save()
    for (i <- units; img <- imageModule.unitGameSymbolDom(i.unitObject).map(_.render)) {
      val height = unitHeight.getOrElse(img.height)
      drawHealthBar(i.unitObject, i.unitState.remainingHealth, drawX(i.x), drawY(i.y), height)
    }
    ctx.restore()
  }

  private def drawHealthBar(unitObject: UnitObject, currentHealth: Int, x: Double, y: Double, objHeight: Double): Unit = {
    ctx.fillStyle = "red"
    ctx.fillRect(x - healthBarWidth / 2, y + healthBarHeight, healthBarWidth, healthBarHeight)
    ctx.fillStyle = "yellow"
    ctx.fillRect(x - healthBarWidth / 2 + 1, y + healthBarHeight + 1, healthBarWidth * currentHealth / unitObject.health - 2, healthBarHeight - 2)
  }

}

object GameCanvas {

  trait ParametersConfig {
    def dotRadius: Int
    def unitHeight: Option[Int]
    def unitWidth: Option[Int]
    def healthBarWidth: Int
    def healthBarHeight: Int
  }

  trait DefaultParametersConfig extends ParametersConfig {
    override def dotRadius: Int = 10
    override def unitHeight: Option[Int] = Some(64)
    override def unitWidth: Option[Int] = Some(64)
    override def healthBarWidth: Int = 50
    override def healthBarHeight: Int = 10
  }


  def defaultCanvas(imageModule: ImageModule,
                    canvas: Canvas,
                    cheight: Int,
                    cwidth: Int,
                    width: Int,
                    laneNumber: Int,
                    selfId: OwnerId) = new GameCanvas(imageModule, canvas, cheight, cwidth, width, laneNumber, selfId) with DefaultParametersConfig {
  }
}
