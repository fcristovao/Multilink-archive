package multilink.game.client

import akka.actor._
import multilink.game.network.internet.InternetPointAddress

object Dialer {
  sealed trait States
  case object Idle extends States
  case object LocatingInternetPoints extends States

  sealed trait Messages
  case class CreateRoute(route: List[InternetPointAddress]) extends Messages
  case class RouteCreated(routeActor: ActorRef) extends Messages
  case class RouteFailed(reason: RouteFailureReason) extends Messages

  sealed trait RouteFailureReason
  case class UnknownInternetPoint(ip: InternetPointAddress) extends RouteFailureReason
  case class IllegalRoute(route: List[InternetPointAddress]) extends RouteFailureReason

  sealed trait Data
  case class WhileLocatingIPs(answerTo: ActorRef, route: List[InternetPointAddress], actors: Map[InternetPointAddress, ActorRef]) extends Data
  case object NullData extends Data
}

class Dialer(internetPointsDatabase: ActorRef) extends Actor with
                                                       FSM[Dialer.States, Dialer.Data] with
                                                       LoggingFSM[Dialer.States, Dialer.Data] {
  import Dialer._
  import InternetPointsDatabase._

  startWith(Idle, NullData)

  when(Idle) {
    case Event(CreateRoute(routingList), _) if routingList.size < 2 =>
      stay replying RouteFailed(IllegalRoute(routingList))
    case Event(CreateRoute(routingList), _) =>
      for (ip <- routingList) {
        internetPointsDatabase ! Get(ip)
      }
      goto(LocatingInternetPoints) using WhileLocatingIPs(sender(), routingList, Map.empty)
  }

  when(LocatingInternetPoints) {
    case Event(IPFor(internetPointAddress, None), WhileLocatingIPs(answerTo, _, _)) =>
      answerTo ! RouteFailed(UnknownInternetPoint(internetPointAddress))
      goto(Idle) using NullData

    case Event(IPFor(internetPointAddress, Some(actor)), data@WhileLocatingIPs(answerTo, routingList, mapOfIPs)) => {
      val newMapOfIPs = mapOfIPs + (internetPointAddress -> actor)

      if (newMapOfIPs.size == routingList.size) {
        val routingActors = for (ip <- routingList; actorRef = newMapOfIPs(ip)) yield {
          (ip, actorRef)
        }
        val route = context.actorOf(Props(classOf[Route], routingActors))
        answerTo ! RouteCreated(route)
        goto(Idle) using NullData
      } else {
        stay using data.copy(actors = newMapOfIPs)
      }
    }
  }

  initialize()
}