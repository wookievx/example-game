package com.example.model

import boopickle.Default._
import com.example.model.units.UnitInstance

import scala.collection.mutable.{Seq => MSeq}

sealed trait GameResponse

object GameResponse {

  case object GameEnded extends GameResponse
  case class UnitResponse(instances: MSeq[UnitInstance]) extends GameResponse
  case class PlayerStateResponse(id: OwnerId, ownState: PlayerState.StateDiff, enemyHealth: Int) extends GameResponse
  case class CommandResponse(command: Command, response: SpecificResponse) extends GameResponse

  implicit val pickler: Pickler[GameResponse] = generatePickler
}
