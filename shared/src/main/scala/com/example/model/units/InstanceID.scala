package com.example.model.units
import boopickle.Default._
case class InstanceID(id: Long) {
  def toStringId: String = String.valueOf(id)
}

object InstanceID {

  trait InstanceIDFactory {
    def next(): InstanceID
  }

  def defaultFactory = new InstanceIDFactory {
    private[this] var max = Long.MaxValue

    override def next(): InstanceID = this.synchronized {
      if (max != Long.MaxValue) {
        max = max + 1
        InstanceID(max)
      } else {
        max = Long.MinValue
        InstanceID(max)
      }
    }
  }

  implicit val pickler: Pickler[InstanceID] = generatePickler[InstanceID]
}
