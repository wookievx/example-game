package com.example.model.units

import boopickle.Default._
import boopickle.Pickler
import play.api.libs.json._

sealed trait DamageType {
  def name: String
}

object DamageType {
  final case object Normal extends DamageType {
    override def name: String = "normal"
  }
  final case object Magic extends DamageType {
    override def name: String = "magic"
  }
  final case object Siege extends DamageType {
    override def name: String = "siege"
  }

  private val damageTypeReads: Reads[DamageType] = Reads {
    case JsString("magic") => JsSuccess(Magic)
    case JsString("normal") => JsSuccess(Normal)
    case JsString("siege") => JsSuccess(Siege)
    case _ => JsError()
  }

  private val damageTypeWrites: Writes[DamageType] = Writes(t => JsString(t.name))

  implicit val format = Format(damageTypeReads, damageTypeWrites)
  implicit val pickler: Pickler[DamageType] = generatePickler[DamageType]
}
