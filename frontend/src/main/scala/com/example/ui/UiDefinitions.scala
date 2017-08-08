package com.example.ui

import com.example.model.OwnerId
import com.example.model.units._
import com.example.styles.Tooltip
import com.example.ui.draw.ImageModule
import org.scalajs.dom.html.Div

import scalacss.ScalatagsCss._
import scalatags.JsDom.TypedTag
import scalatags.JsDom.all._

object UiDefinitions {

  def createTooltip(units: Seq[UnitObject], module: ImageModule, id: OwnerId, createCallback: UnitObject => Unit): TypedTag[Div] = {
    val elems = units.map(unitImage(module, createCallback))
    div(elems)
  }

  private def unitImage(module: ImageModule, callback: UnitObject => Unit)(unit: UnitObject): TypedTag[Div] = {
    div(Tooltip.tooltip)(
      module.unitIconDom(height := "40px", width := "40px", onclick := {() => callback(unit)})(unit),
      div(`class` := Tooltip.tooltiptext)(
        ul(
          li(s"health: ${unit.health}"),
          li(s"armor type: ${unit.armorType.name}"),
          li(s"damage: ${unit.damage}"),
          li(s"damage type: ${unit.damageType.name}")
        )
      )
    )
  }

}
