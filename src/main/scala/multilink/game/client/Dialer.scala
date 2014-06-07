package multilink.game.client

import akka.actor._
import multilink.game.network.intranet.Gateway
import multilink.game.network.internet.InternetPointAddress
import scala.collection.immutable.Queue

object Dialer {
  sealed trait States
  case object Idle extends States
  case object Connecting extends States
  case object WaitingGatewaysConfirmation extends States
  case object Connected extends States

  sealed trait Messages
  case class Connect(route: List[InternetPointAddress]) extends Messages
  case object Disconnect extends Messages
  case object ConnectionEstablished extends Messages
  case class ConnectionFailed(ip: InternetPointAddress) extends Messages

  sealed trait Data
  case class WhileDialing(answerTo: ActorRef, messages: Map[InternetPointAddress, Gateway.Messages], waitingFor: Set[ActorRef], gatewayAnswers: Queue[(ActorRef, Gateway.Messages)]) extends Data
  case class WhileWaitingGatewaysConfirmation(answerTo: ActorRef, waitingFor: Set[ActorRef]) extends Data
  case object NullData extends Data
}

class Dialer(internetPointsDatabase: ActorRef) extends Actor with
                                                       FSM[Dialer.States, Dialer.Data] with
                                                       LoggingFSM[Dialer.States, Dialer.Data] {
  import Dialer._
  import InternetPointsDatabase._
  import scala.concurrent.duration._

  def getActorRefs(routingList: List[InternetPointAddress]): Map[InternetPointAddress, Gateway.Messages] = {
    def helper(routingList: List[InternetPointAddress], state: Map[InternetPointAddress, Gateway.Messages]): Map[InternetPointAddress, Gateway.Messages] = {
      routingList match {
        case from :: (rest@through :: to :: _) => {
          internetPointsDatabase ! Get(through)
          helper(rest, state + (through -> Gateway.Route(from, through, to)))
        }
        case from :: to :: Nil => {
          internetPointsDatabase ! Get(to)
          state + (to -> Gateway.Connect(from, to))
        }
        case _ => throw new IllegalArgumentException("Routing list cannot be less than 2 Internet Points")
      }
    }

    helper(routingList, Map.empty)
  }

  startWith(Idle, NullData)

  when(Idle) {
    case Event(Connect(routingList), _) =>
      goto(Connecting) using WhileDialing(sender(), getActorRefs(routingList), Set.empty, Queue.empty)
  }

  when(Connecting, 5 seconds) {
    case Event(IPFor(id, None), WhileDialing(answerTo, _, _, _)) =>
      answerTo ! ConnectionFailed(id)
      goto(Idle) using NullData

    case Event(IPFor(id, Some(actor)), data@WhileDialing(answerTo, mapOfIDs, waitingForGateways, _)) => {
      val newWaitingForGateways = waitingForGateways + actor
      val tmp = mapOfIDs - id

      actor ! mapOfIDs(id)

      if (tmp.isEmpty) {
        goto(WaitingGatewaysConfirmation) using WhileWaitingGatewaysConfirmation(answerTo, newWaitingForGateways)
      } else {
        stay using data.copy(messages = tmp, waitingFor = newWaitingForGateways)
      }
    }

    case Event(msg: Gateway.Messages, data@WhileDialing(_, _, _, gatewayAnswers)) =>
      stay using data.copy(gatewayAnswers = gatewayAnswers.enqueue(sender -> msg))

    case Event(StateTimeout, _) =>
      goto(Idle) using NullData
  }

  when(WaitingGatewaysConfirmation, 5 seconds) {
    case Event(msg: Gateway.Messages, WhileWaitingGatewaysConfirmation(answerTo, waitingForGateways)) => {
      val newWaitingForGateways = waitingForGateways - sender
      if (newWaitingForGateways.isEmpty) {
        answerTo ! ConnectionEstablished
        goto(Connected) using NullData
      } else {
        stay using WhileWaitingGatewaysConfirmation(answerTo, newWaitingForGateways)
      }
    }

    case Event(StateTimeout, _) => goto(Idle) using NullData
  }

  when(Connected, 5 seconds) {
    case Event(Disconnect, _) => goto(Idle) using NullData
    case Event(StateTimeout, _) => goto(Idle) using NullData
  }

  initialize()
}