package services

import com.example.model.units.UnitObject._
import com.example.model.units.UnitTraits

class AssetResolver(prefix: String) {

  def getIconName(unitType: UnitTraits): String = unitType match {
    case _: Swordsman => "sword.png"
    case _: Archer => "bow.png"
    case _: Catapult => "catapult.png"
    case _: Wizard => "staff.png"
    case _: Tower => "tower.png"
  }

  def getPrefixedIconName(unitType: UnitTraits): String = s"$prefix/${getIconName(unitType)}"

  def getUnitSymbol(unitType: UnitTraits): String = unitType match {
    case _: Swordsman => "swordsman.png"
    case _: Archer => "archer.png"
    case _: Wizard => "wizard.png"
    case _ => "unknown"
  }

  def getPrefixedUnitSymbol(unitType: UnitTraits): String = s"$prefix/${getUnitSymbol(unitType)}"
}
