package com.example.model
import boopickle.Default._
import boopickle.Pickler
import com.example.model.units.UnitObject

sealed trait Command

object Command {

  case class CreateUnit(obj: UnitObject) extends Command
  case class CanCreateUnit(obj: UnitObject) extends Command
  case object NextLane extends Command

  implicit val pickler: Pickler[Command] = generatePickler[Command]


}
