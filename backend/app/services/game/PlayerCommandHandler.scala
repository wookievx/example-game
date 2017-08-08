package services.game

import com.example.config.GameConfig
import com.example.model.Command._
import com.example.model.GameResponse._
import com.example.model.SpecificResponse._
import com.example.model._
import com.example.model.units.InstanceID.InstanceIDFactory
import com.example.model.units.UnitInstance
import com.example.model.units.UnitState._

class PlayerCommandHandler(config: GameConfig, instanceIDFactory: InstanceIDFactory) {
  private val logger = play.api.Logger(getClass)

  def handleCommand(ownerId: OwnerId, playerState: PlayerState, event: Command)(playerReporter: UnsafeActorRef[GameResponse]): PlayerState =
    event match {
      case c@CanCreateUnit(obj) =>
        if (playerState.gold > config.unitValue(obj) && playerState.unitCount < config.unitLimit) {
          playerReporter ! CommandResponse(c, UnitCreationPossible)
        } else {
          playerReporter ! CommandResponse(c, UnitCreationImpossible)
        }
        playerState
      case c@CreateUnit(obj) =>
        if (playerState.gold > config.unitValue(obj) && playerState.unitCount < config.unitLimit) {
          val q = playerState.withUnitInQueue(UnitInstance(instanceIDFactory.next(), ownerId, obj, 0, playerState.currentLane, obj.moving))
          val next = if (q.currentLane < config.laneNumber - 1) {
            q.copy(currentLane = q.currentLane + 1).withGold(_ - config.unitValue(obj))
          } else {
            q.copy(currentLane = 0)
          }
          playerReporter ! CommandResponse(c, LaneAssigned(next.currentLane))
          next
        } else {
          playerReporter ! CommandResponse(c, UnitCreationFailed)
          playerState
        }
      case NextLane =>
        val next = playerState.currentLane
        playerReporter ! CommandResponse(NextLane, LaneAssigned(next))
        playerState
    }


}
