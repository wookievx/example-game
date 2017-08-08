package com.example.config

import com.example.model.OwnerId.OwnerIdFactory
import com.example.model.units.UnitObject
import com.example.model.units.InstanceID.InstanceIDFactory

trait GameConfig {
  final def laneNumber: Int = 8
  def unitLimit: Int
  def availableUnits: Set[UnitObject]
  def unitDimensions(unitObject: UnitObject): (Int, Int)
  def unitValue(unitObject: UnitObject): Int
  def width: Int
  def height: Int
  def spawnOffset: Int
  def startGold: Int
  def baseHealth: Int
  def goldIncome: Int
  def instanceIdFactory: InstanceIDFactory
  def ownerIdFactory: OwnerIdFactory
}
