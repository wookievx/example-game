package com.example.ui.draw

import com.example.config.Entry
import com.example.model.units.UnitTraits
import org.scalajs.dom.html.Image

import scala.language.implicitConversions
import scalatags.JsDom.TypedTag
import scalatags.JsDom.all._
import scalatags.JsDom.Modifier
import org.scalajs.dom

import scala.collection.mutable.{Map => MMap}

trait ImageModule {
  def unitIcon(unitTraits: UnitTraits): Option[String]
  def unitGameSymbol(unitTraits: UnitTraits): Option[String]

  def unitIconDom(modifiers: Modifier*)(unitTraits: UnitTraits) : Option[TypedTag[Image]] = {
    unitIcon(unitTraits).map(s => img(modifiers:_*)(src := s))
  }
  def unitGameSymbolDom(unitTraits: UnitTraits): Option[TypedTag[Image]] = {
    unitGameSymbol(unitTraits).map(s => img(src := s))
  }
}

object ImageModule {

  def fromArguments(iconEntries: Array[Entry[String]], symbolEntries: Array[Entry[String]]): ImageModule {
    def unitIcon(unitTraits: UnitTraits): Option[String]

    def unitGameSymbol(unitTraits: UnitTraits): Option[String]
  } = {
    println(iconEntries.mkString("[", ", ", "]"))
    println(symbolEntries.mkString("[", ", ", "]"))
    new ImageModule {
      override def unitGameSymbol(unitTraits: UnitTraits): Option[String] = symbolEntries.collectFirst {
        case Entry(k, v) if k == unitTraits.name => v
      }

      override def unitIcon(unitTraits: UnitTraits): Option[String] = iconEntries.collectFirst {
        case Entry(k, v) if k == unitTraits.name => v
      }
    }
  }

  def fromArgumentsLogging(iconEntries: Array[Entry[String]], symbolEntries: Array[Entry[String]]): ImageModule = {
    new ImageModule {
      private val inner = fromArguments(iconEntries, symbolEntries)
      override def unitGameSymbol(unitTraits: UnitTraits): Option[String] = {
        println(s"Unit name: ${unitTraits.name}, entries: ${symbolEntries.mkString("[ ", ", ", "]")}")
        val foundEntry = inner.unitGameSymbol(unitTraits)
        println(s"Found: $foundEntry")
        foundEntry
      }
      override def unitIcon(unitTraits: UnitTraits): Option[String] = {
        println(s"Unit name: ${unitTraits.name}, entries: ${iconEntries.mkString("[ ", ", ", "]")}")
        val foundEntry = inner.unitIcon(unitTraits)
        println(s"Found: $foundEntry")
        foundEntry
      }
    }
  }


  implicit def toExtendedModule(module: ImageModule): ExtendedModule = new ExtendedModule(module)

  class ExtendedModule(private val module: ImageModule) extends AnyVal {
    def withDefaultIcon(source: String) = new ImageModule {
      override def unitGameSymbol(unitTraits: UnitTraits): Option[String] = module.unitGameSymbol(unitTraits)
      override def unitIcon(unitTraits: UnitTraits): Option[String] = module.unitIcon(unitTraits).orElse(Some(source))
    }

    def withDefaultSymbol(source: String) = new ImageModule {
      override def unitGameSymbol(unitTraits: UnitTraits): Option[String] = module.unitGameSymbol(unitTraits).orElse(Some(source))
      override def unitIcon(unitTraits: UnitTraits): Option[String] = module.unitIcon(unitTraits)
    }

    def withDefaults(iconSource: String, symbolSource: String) = new ImageModule {
      override def unitGameSymbol(unitTraits: UnitTraits): Option[String] = module.unitGameSymbol(unitTraits).orElse(Some(symbolSource))
      override def unitIcon(unitTraits: UnitTraits): Option[String] = module.unitIcon(unitTraits).orElse(Some(iconSource))
    }
  }



}
