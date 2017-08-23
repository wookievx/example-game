package com.example.model
import boopickle.Default._

sealed trait LobbyMessage

object LobbyMessage {
  final case object Started extends LobbyMessage
  final case object Canceled extends LobbyMessage

  implicit val pickler: Pickler[LobbyMessage] = generatePickler
}
