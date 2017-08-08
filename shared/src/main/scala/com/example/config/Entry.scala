package com.example.config

import play.api.libs.json._

case class Entry[T](key: String, value: T)

object Entry {
  implicit def format[T:Format]: Format[Entry[T]] = Json.format[Entry[T]]
}
