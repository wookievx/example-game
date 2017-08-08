package com.example.model.units

import java.util.UUID

import boopickle.Default._
import boopickle.Pickler
import com.example.model.OwnerId

import scala.language.implicitConversions

case class UnitInstance(instanceID: InstanceID, ownerId: OwnerId, unitObject: UnitObject, x: Int = 0, y: Int = 0, unitState: UnitState)

object UnitInstance {
  implicit val pickler: Pickler[UnitInstance] = generatePickler[UnitInstance]
  implicit val ordering = new Ordering[UnitInstance] {
    override def compare(x: UnitInstance, y: UnitInstance): Int = x.x - y.x
  }

  /**
    * Implicit conversion allowing for using int for querying range of instances
    * @param x int to query
    * @return most
    */
  implicit def intToInstance(x: Int): UnitInstance = UnitInstance(InstanceID(-1), OwnerId(-1), UnitObject.wizard.asInstanceOf[UnitObject], x, 0, UnitState.Dead)
}
