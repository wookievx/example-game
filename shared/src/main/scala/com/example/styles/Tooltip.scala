package com.example.styles

import CssSettings._

import scala.language.postfixOps

object Tooltip extends StyleSheet.Inline {
  import dsl._

  val tooltip = style(
    margin(0 px, 10 px, 0 px, 0 px),
    position.relative,
    display.inlineBlock,
    cursor.pointer,
    unsafeChild(`.tooltiptext`)(
      visibility.hidden,
      width(200 px),
      backgroundColor.lightblue,
      color.darkred,
      position.absolute,
      zIndex(1),
    ),
    &.hover(
      unsafeChild(`.tooltiptext`)(
        visibility.visible
      )
    )
  )

  def tooltiptext = "tooltiptext"
  private def `.tooltiptext` = s".$tooltiptext"

  def main(args: Array[String]): Unit = {
    println(render[String])
  }

}
