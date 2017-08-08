package com.example

import com.example.components.BasicComponents

import scala.scalajs.js.timers._
import org.scalajs.dom.html.Canvas
import org.scalajs.dom.raw.{CanvasRenderingContext2D, HTMLImageElement}
import com.example.config.Entry
import com.example.model.units.UnitObject._
import org.scalajs.dom

import scala.concurrent.{Future, Promise}
import scala.concurrent.duration._
import scala.scalajs.js.JSApp
import scala.util.{Failure, Success}

object ScalaJSExample extends JSApp {

  import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

  class Image(src: String) {
    private val element = dom.document.createElement("img").asInstanceOf[HTMLImageElement]
    private val p = Promise[HTMLImageElement]
    element.onload = _ => p.complete(Success(element))
    element.src = src

    def getContent: Future[HTMLImageElement] = p.future
  }


  def show(args: Array[Entry[String]]): Unit = {

    val images = args.collect {
      case Entry(n, path) if n == swordsman.name =>
        swordsman -> new Image(path)
      case Entry(n, path) if n == archer.name =>
        archer -> new Image(path)
      case Entry(n, path) if n == wizard.name =>
        wizard -> new Image(path)
    }

    val canvas = dom.document.createElement("canvas").asInstanceOf[Canvas]
    val ctx = canvas.getContext("2d").asInstanceOf[CanvasRenderingContext2D]

    canvas.width = (0.5 * dom.window.innerWidth).toInt
    canvas.height = (0.5 * dom.window.innerHeight).toInt
    dom.document.getElementById(BasicComponents.gameArea).appendChild(canvas)

    def render(): Unit = {
      for (((_, img), index) <- images.zipWithIndex) {
        img.getContent.onComplete {
          case Success(elem) => ctx.drawImage(elem, index * 100.0, 100.0, 50.0, 50.0)
          case Failure(err) =>
            ctx.fillStyle = "rgb(250, 250, 250)"
            ctx.font = "24px Helvetica"
            ctx.textAlign = "left"
            ctx.textBaseline = "top"
            ctx.fillText("ERROR", index * 100.0, 100.0)
        }
      }
    }
    setTimeout(1.second) {
      render()
    }
  }

  def showDom(args: Array[Entry[String]]): Unit = {


    val images = args.collect {
      case Entry(n, path) if n == swordsman.name =>
        swordsman -> new Image(path)
      case Entry(n, path) if n == archer.name =>
        archer -> new Image(path)
      case Entry(n, path) if n == wizard.name =>
        wizard -> new Image(path)
    }


  }

  def main(): Unit = {
    show {
      Array(Entry(swordsman.name, "assets/images/sword.png"), Entry(archer.name, "assets/images/bow.png"), Entry(wizard.name, "assets/images/staff.png"))
    }
  }
}
