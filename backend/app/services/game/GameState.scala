package services.game

import com.example.model._
import com.example.model.units.UnitInstance

import scala.collection.SortedSet

sealed trait GameState

case class OnePlayerJoined(player: OwnerId) extends GameState

case class Ongoing(player1: OwnerId,
                   player2: OwnerId,
                   player1State: PlayerState,
                   player2State: PlayerState,
                   units: Map[Int, SortedSet[UnitInstance]]) extends GameState



