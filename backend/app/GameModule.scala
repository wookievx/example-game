import akka.actor.ActorSystem
import akka.util.Timeout
import com.example.config.GameConfig
import com.example.model.OwnerId
import com.example.model.OwnerId.OwnerIdFactory
import com.example.model.units.{InstanceID, UnitObject}
import com.example.model.units.InstanceID.InstanceIDFactory
import com.example.model.units.UnitObject.{Archer, Swordsman, Wizard}
import controllers.{GameController, GreeterController, MiscController}
import play.api.i18n.Langs
import play.api.mvc.ControllerComponents
import services.game.{GameRepository, GameServer}
import services.{AssetResolver, ServicesModule}

import scala.concurrent.duration._

trait GameModule extends ServicesModule {

  import com.softwaremill.macwire._

  def actorSystem: ActorSystem
  lazy val timeout: Timeout = Timeout(2.second)
  lazy val greeterController: GreeterController = wire[GreeterController]
  lazy val server: GameServer = wire[GameServer]
  lazy val assetResolver: AssetResolver = new AssetResolver("images")
  lazy val instanceIDFactory: InstanceIDFactory = InstanceID.defaultFactory
  lazy val ownerIdFactory: OwnerIdFactory = OwnerId.defaultFactory
  lazy val gameController: GameController = wire[GameController]
  lazy val miscController: MiscController = wire[MiscController]

  lazy val gameConfig = new GameConfig {
    override def baseHealth: Int = 10000
    override def startGold: Int = 300
    override def spawnOffset: Int = 50
    override val instanceIdFactory: InstanceIDFactory = InstanceID.defaultFactory
    override def width: Int = 1000
    override def unitDimensions(unitObject: UnitObject): (Int, Int) = (40, 1)
    override def goldIncome: Int = 1
    override def height: Int = 8
    override def unitLimit: Int = 20
    override def unitValue(unitObject: UnitObject): Int = 100
    override def availableUnits: Set[UnitObject] = Set(
      Swordsman(100, 10, 1.second, 50),
      Archer(80, 15, 2.seconds, 150),
      Wizard(70, 15, 2.seconds, 170)
    )

    override val ownerIdFactory: OwnerIdFactory = OwnerId.defaultFactory
  }

  def langs: Langs

  def controllerComponents: ControllerComponents
}
