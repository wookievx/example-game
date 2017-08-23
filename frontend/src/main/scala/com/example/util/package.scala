package com.example

import scala.language.implicitConversions
import scala.scalajs.js

package object util {


  @js.native
  trait PlayCall extends js.Object {
    def method: String = js.native
    def url: String = js.native
    def absoluteURL: String = js.native
    def webSocketURL(): String = js.native
  }

  /**
    * Unsafe way of accessing play call route, but lack of alternative
    */
  implicit def jsDynamicToPlayCall(d: Dynamic): PlayCall = d.asInstanceOf[PlayCall]
}
