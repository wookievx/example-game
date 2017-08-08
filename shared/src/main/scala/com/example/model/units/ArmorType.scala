package com.example.model.units

import boopickle.Default._
import boopickle.Pickler
import play.api.libs.json._

sealed trait ArmorType {
  def name: String
}

object ArmorType {
  final case object Light extends ArmorType {
    override def name: String = "light"
  }
  final case object Heavy extends ArmorType {
    override def name: String = "heavy"
  }
  final case object Fortified extends ArmorType {
    override def name: String = "fortified"
  }

  private val damageTypeReads: Reads[ArmorType] = Reads {
    case JsString("light") => JsSuccess(Light)
    case JsString("heavy") => JsSuccess(Heavy)
    case JsString("fortified") => JsSuccess(Fortified)
    case _ => JsError()
  }

  private val damageTypeWrites: Writes[ArmorType] = Writes(t => JsString(t.name))

  implicit val format = Format(damageTypeReads, damageTypeWrites)
  implicit val pickler: Pickler[ArmorType] = generatePickler[ArmorType]
}
