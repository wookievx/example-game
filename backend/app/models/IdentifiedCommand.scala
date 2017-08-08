package models

import com.example.model.{Command, OwnerId}

case class IdentifiedCommand(ownerId: OwnerId, playerId: OwnerId, command: Command)
