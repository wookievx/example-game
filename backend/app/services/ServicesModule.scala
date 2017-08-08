package services

import services.game.NameService

trait ServicesModule {
  import com.softwaremill.macwire._

  lazy val nameModule: NameService = wire[NameService]
}
