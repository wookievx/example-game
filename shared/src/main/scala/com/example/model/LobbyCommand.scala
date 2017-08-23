package com.example.model
import boopickle.Default._

sealed trait LobbyCommand
object LobbyCommand {
  final case object Cancel extends LobbyCommand
  final case class Close(successful: Boolean) extends LobbyCommand
  implicit val pickler: Pickler[LobbyCommand] = generatePickler
}
