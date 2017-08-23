package com.example.styles

import CssSettings._


object JsButton extends StyleSheet.Inline {
  import dsl._

  val basicButton = style(
    padding(2 px, 2 px, 2 px, 2 px),
    cursor.pointer,
    backgroundColor.lightblue,
    color.darkred
  )

}
