package services.game

import com.example.config.GameConfig
import com.example.model.{OwnerId, PlayerState}
import com.example.model.units.{InstanceID, UnitInstance, UnitObject}
import InstanceID._
import OwnerId._
import com.example.model.units.UnitState._
import org.scalatest._

import scala.collection.SortedSet

class CollisionCheckerTest extends FlatSpec with Matchers {

  private val testConfig = new GameConfig {
    override def availableUnits: Set[UnitObject] = Set.empty
    override def baseHealth: Int = 1000
    override def startGold: Int = 0
    override def ownerIdFactory: OwnerIdFactory = OwnerId.defaultFactory
    override def spawnOffset: Int = 100
    override def instanceIdFactory: InstanceIDFactory = InstanceID.defaultFactory
    override def width: Int = 1000
    override def unitDimensions(unitObject: UnitObject): (Int, Int) = 30 -> 1
    override def goldIncome: Int = 1000
    override def height: Int = 1
    override def unitLimit: Int = 10000
    override def unitValue(unitObject: UnitObject): Int = 1000
  }

  private val testChecker = new CollisionChecker {
    override val gameConfig: GameConfig = testConfig
  }

  private val selfId = OwnerId(1)
  private val opponentId = OwnerId(2)
  @inline
  private implicit class RInt(private val i: Int) {
    def toSelfInstance: UnitInstance = UnitInstance(
      testConfig.instanceIdFactory.next(),
      selfId,
      UnitObject.swordsman.asInstanceOf[UnitObject],
      i,
      0,
      Moving(100)
    )
    def toOpponentInstance: UnitInstance = toSelfInstance.copy(ownerId = opponentId)
  }

  "unit in range checking" should "detect collisions" in {
    val coll1 = 100.toSelfInstance
    val coll2 = 300.toSelfInstance
    val exampleUnits = Map(0 -> SortedSet[UnitInstance](coll1, 150.toSelfInstance, coll2, 400.toSelfInstance))
    val exampleState = PlayerState.fromGameConfig(testConfig)
    val state = Ongoing(selfId, opponentId, exampleState, exampleState, exampleUnits)
    val colliding = testChecker.checkUnitRange(Set(selfId, opponentId), 0, 25.toSelfInstance, 75, state)
    val colliding2 = testChecker.checkUnitRange(Set(selfId, opponentId), 0, 225.toSelfInstance, 100, state)
    colliding should equal(Some(coll1))
    colliding2 should equal(Some(coll2))
  }

  "attack detecting"  should "work" in {
    val coll1 = 100.toOpponentInstance
    val coll2 = 300.toOpponentInstance
    val exampleUnits = Map(0 -> SortedSet[UnitInstance](coll1, 150.toSelfInstance, coll2, 400.toSelfInstance))
    val exampleState = PlayerState.fromGameConfig(testConfig)
    val state = Ongoing(selfId, opponentId, exampleState, exampleState, exampleUnits)
    val colliding1 = testChecker.checkUnitRange(Set(opponentId), 0, 25.toSelfInstance, 75, state)
    val colliding2 = testChecker.checkUnitRange(Set(opponentId), 0, 225.toSelfInstance, 100, state)
    colliding1 should equal(Some(coll1))
    colliding2 should equal(Some(coll2))
  }


  "in this situation" should "detect blocking" in {

    val exampleUnits = Map(0 -> SortedSet[UnitInstance](20 to 320 by 30 map {_.toSelfInstance} :_*))
    val exampleState = PlayerState.fromGameConfig(testConfig)
    val state = Ongoing(selfId, opponentId, exampleState, exampleState, exampleUnits)
    val afterBlocking = testChecker.handleBlocking(selfId, state)
    afterBlocking.units(0).foreach { i => i.unitState.isInstanceOf[Blocked] should be(true)}

  }


}
