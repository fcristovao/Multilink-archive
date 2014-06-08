package multilink.game.client

import multilink.game.network.internet._
import akka.actor.{LoggingFSM, FSM, Actor, ActorRef}
import multilink.game.client.Route._
import multilink.game.network.intranet.Gateway
import scala.concurrent.duration._

object Route {
  sealed trait States
  case object Ready extends States
  case object WaitingGatewaysConfirmation extends States
  case object Connected extends States

  sealed trait Messages
  case object Connect extends Messages
  case class ConnectionEstablished() extends Messages
  case class ConnectionFailed() extends Messages

  sealed trait Data
  case class WhileWaitingGatewaysConfirmation(answerTo: ActorRef, gateways: Set[ActorRef]) extends Data
  case object NullData extends Data
}

class Route(routingSeq: Seq[(InternetPointAddress, ActorRef)]) extends Actor with
                                                                       FSM[Route.States, Route.Data] with
                                                                       LoggingFSM[Route.States, Route.Data] {
  require(routingSeq.size >= 2, "Routing list must contain 2 or more points")

  def sendGatewayRequests(routingSeq: Seq[(InternetPointAddress, ActorRef)]) {
    routingSeq match {
      case Seq((from, _), (through, actorThrough), (to, _), _*) =>
        actorThrough ! Gateway.Route(from, through, to)
        sendGatewayRequests(routingSeq.tail)
      case Seq((from, _), (to, actorTo)) =>
        actorTo ! Gateway.Connect(from, to)
      case _ => throw new Exception("this should never happen")
    }
  }

  startWith(Ready, NullData)

  when(Ready) {
    case Event(Connect, _) =>
      sendGatewayRequests(routingSeq)
      goto(WaitingGatewaysConfirmation) using WhileWaitingGatewaysConfirmation(sender(), Set())
  }

  when(WaitingGatewaysConfirmation, 5 seconds) {
    case Event(msg: Gateway.Messages, WhileWaitingGatewaysConfirmation(answerTo, answeredGateways)) => {
      val newAnsweredGateways = answeredGateways + sender
      if (newAnsweredGateways.size == routingSeq.size - 1) {
        answerTo ! ConnectionEstablished()
        goto(Connected) using NullData
      } else {
        stay using WhileWaitingGatewaysConfirmation(answerTo, newAnsweredGateways)
      }
    }

    case Event(StateTimeout, WhileWaitingGatewaysConfirmation(answerTo, _)) =>
      answerTo ! ConnectionFailed
      stop()
  }

  when(Connected, 5 seconds) {
    case Event(StateTimeout, _) =>
      stop()
  }

  initialize()
}
