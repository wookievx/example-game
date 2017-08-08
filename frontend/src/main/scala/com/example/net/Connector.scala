package com.example.net

import com.example.model.GameResponse._
import com.example.model._
import monix.reactive._

import scala.concurrent.Future

trait Connector {
  def playerID: OwnerId
  def gameID: OwnerId
  def unitStream: Observable[UnitResponse]
  def playerStateStream: Observable[PlayerStateResponse]
  def endGameResponse(): Future[Unit]
  def request(input: Command): Future[CommandResponse]
  def connect(): Unit
}
