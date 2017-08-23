package services.game

import akka.actor.Props
import akka.typed.scaladsl.Actor
import akka.actor.{Actor => UnsafeActor}
import akka.typed.{ActorRef, ActorSystem, Behavior, Terminated}
import com.example.config.GameConfig
import com.example.model.OwnerId
import services.game.GameHandler._
import services.game.GameRepository._
import services.game.ai.AiAdapter
import services.game.ai.AiAdapter._

class GameServer(nameService: NameService, gameConfig: GameConfig, utilSystem: akka.actor.ActorSystem) {
  private[this] implicit val us = utilSystem

  import gameConfig._

  private val sinkRef = utilSystem.actorOf(Props(new UnsafeActor {
    override def receive: Receive = {
      case _ =>
    }
  }))

  private def behavior: Behavior[GameMessage] = Actor.deferred { ctx =>
    val gameRepository = new GameRepository(gameConfig)
    val gameRepositoryRef = ctx.spawn(gameRepository.behavior(Map.empty), "game-repository")
    ctx.watch(gameRepositoryRef)

    def defaultServerBehavior(ai: Map[OwnerId, ActorRef[AIAdapterMessage]]): Behavior[GameMessage] = Actor.immutable[GameMessage] { (ctx, msg) =>
      val nextAI = if (ai.nonEmpty) ai else {
        val id = ownerIdFactory.next()
        val aiName = s"AI-{${id.id}}"
        val ref = ctx.spawn(new AiAdapter(gameConfig, gameRepositoryRef).mainBehavior(id), s"ai-wrapper:${id.id}")
        ctx.watch(ref)
        gameRepositoryRef ! CreateEvent(id, AI(aiName), sinkRef)
        gameRepositoryRef ! JoinEvent(id, id, ref.toNarrowingActor(Left(_)))
        nameService.save(id, aiName)
        ai + (id -> ref)
      }

      msg match {
        case Right(CreateEvent(id, gt, ref)) =>
          gameRepositoryRef ! CreateEvent(id, gt, ref)
          defaultServerBehavior(nextAI)
        case Right(JoinEvent(ownerId, playerId, ref)) =>
          nextAI.get(ownerId).foreach(_ ! StartGame(ownerId, playerId, ref).toRight)
          gameRepositoryRef ! JoinEvent(ownerId, playerId, ref).toRight
          defaultServerBehavior(nextAI)
        case Right(e@LeaveEvent(ownerId)) =>
          nextAI.get(ownerId).foreach(_ ! Kill(ownerId).toRight)
          gameRepositoryRef ! e.toRight
          defaultServerBehavior(nextAI - ownerId)
        case Right(ce: CommandEvent) =>
          gameRepositoryRef ! ce.toRight
          defaultServerBehavior(nextAI)
        case Left(q) =>
          gameRepositoryRef ! q
          defaultServerBehavior(nextAI)
      }

    } onSignal {
      case (con, t@Terminated(ref)) if t.wasFailed =>
        con.system.log.error("Worker {} is TERMINATED with error {}", ref, t.failure)
        defaultServerBehavior(ai)
      case (con, Terminated(ref)) =>
        con.system.log.info("Worker {} is TERMINATED", ref)
        defaultServerBehavior(ai)
    }

    defaultServerBehavior(Map.empty)
  }

  private lazy val system = ActorSystem("server-main", behavior)
  val actorRef: ActorRef[GameMessage] = system
  val unsafeSource: UnsafeActorRef[GameEvent] = system.toNarrowingActor[GameEvent](_.toRight)

}

object GameServer {

  case object CreateAIGame

}
