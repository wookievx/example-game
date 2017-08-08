package com.example

import play.api.libs.json._

case class SomeData(x: Int, y: Int, z: Int)

object SomeData {
  def format: OFormat[SomeData] = Json.format[SomeData]

  implicit val implFormat: OFormat[SomeData] = format
}
