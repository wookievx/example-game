package com.example.model.units

import java.util.concurrent.TimeUnit

import boopickle.Default._
import boopickle.Pickler
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.concurrent.duration._
import DamageType._
import ArmorType._
import scala.util.Try

trait UnitTraits {
  def name: String
  def armorType: ArmorType
  def damageType: DamageType
}

sealed trait UnitObject extends UnitTraits {
  def health: Int
  def damage: Int
  def movementSpeed: Int
  def attackInterval: FiniteDuration
  def attackRange: Int
}

object UnitObject {

  def damageMultiplication(at: ArmorType, dt: DamageType): BigDecimal = (dt, at) match {
    case (Normal, Light) => BigDecimal(150) / 100
    case (Normal, Heavy) => BigDecimal(1)
    case (Normal, Fortified) => BigDecimal(50) / 100
    case (Magic, Light) => BigDecimal(75) / 100
    case (Magic, Fortified) => BigDecimal(75) / 100
    case (Magic, Heavy) => BigDecimal(150) / 100
    case (Siege, Light) => BigDecimal(50) / 100
    case (Siege, Heavy) => BigDecimal(75) / 100
    case (Siege, Fortified) => BigDecimal(175) / 100
  }

  case class Archer(health: Int, damage: Int, attackInterval: FiniteDuration, attackRange: Int) extends UnitObject {
    override def name: String = "archer"
    override def armorType: ArmorType = Light
    override def damageType: DamageType = Normal
    override def movementSpeed: Int = 2
  }
  val archer: UnitTraits = Archer(-1, -1, 1.second, -1)

  case class Swordsman(health: Int, damage: Int, attackInterval: FiniteDuration, attackRange: Int) extends UnitObject {
    override def name: String = "swordsman"
    override def armorType: ArmorType = Heavy
    override def damageType: DamageType = Normal
    override def movementSpeed: Int = 2
  }

  val swordsman: UnitTraits = Swordsman(-1, -1, 1.second, 100)

  case class Wizard(health: Int, damage: Int, attackInterval: FiniteDuration, attackRange: Int) extends UnitObject {
    override def name: String = "wizard"
    override def armorType: ArmorType = Light
    override def damageType: DamageType = Magic
    override def movementSpeed: Int = 3
  }

  val wizard: UnitTraits = Wizard(-1, -1, 1.second, -1)

  case class Tower(health: Int, damage: Int, attackInterval: FiniteDuration, attackRange: Int) extends UnitObject {
    override def name: String = "tower"
    override def armorType: ArmorType = Fortified
    override def damageType: DamageType = Magic
    override def movementSpeed: Int = 0
  }

  val tower: UnitTraits = Tower(-1, -1, 1.second, -1)

  case class Catapult(health: Int, damage: Int, attackInterval: FiniteDuration, attackRange: Int) extends UnitObject {
    override def name: String = "catapult"
    override def armorType: ArmorType = Heavy
    override def damageType: DamageType = Siege
    override def movementSpeed: Int = 20
  }

  val catapult: UnitTraits = Catapult(-1, -1, 1.second, -1)

  private implicit val timeUnitFormat = Format[TimeUnit](
    Reads {
      case JsString(s) =>
        Try{TimeUnit.valueOf(s)}.map(JsSuccess(_)).getOrElse(JsError())
      case _ =>
        JsError()
    },
    Writes(tu => JsString(tu.name()))
  )

  private implicit val durationFormat: Format[FiniteDuration] = (
    (JsPath \ "count").format[Long] and (JsPath \ "unit").format[TimeUnit]
  )(FiniteDuration.apply, d => d)

  implicit val pickler: Pickler[UnitObject] = generatePickler[UnitObject]
}
