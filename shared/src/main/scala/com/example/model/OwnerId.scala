package com.example.model

import boopickle.DefaultBasic._
import play.api.libs.json._

case class OwnerId(id: Int)

object OwnerId {

  trait OwnerIdFactory {
    def next(): OwnerId
  }

  def defaultFactory = new OwnerIdFactory {
    private var max = Int.MaxValue
    override def next(): OwnerId = this.synchronized {
      if (max != Int.MaxValue) {
        max = max + 1
      } else {
        max = Int.MinValue
      }
      OwnerId(max)
    }
  }

  implicit val pickler: Pickler[OwnerId] = transformPickler[OwnerId, Int](OwnerId(_))(_.id)
  implicit val format: Format[OwnerId] = Json.format[OwnerId]
}
