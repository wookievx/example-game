package com.example.model

import boopickle.Default._

sealed trait SpecificResponse
object SpecificResponse {

  //answers for CreateUnit command
  case object UnitCreationSuccessful extends SpecificResponse
  case object UnitCreationFailed extends SpecificResponse
  //answers for CanCreateUnit command
  case object UnitCreationPossible extends SpecificResponse
  case object UnitCreationImpossible extends SpecificResponse
  //answer for NextLane command
  case class LaneAssigned(laneAssigned: Int) extends SpecificResponse

  implicit val specificResponsePickler: Pickler[GameResponse] = generatePickler
}
