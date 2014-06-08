package multilink.network.intranet

import akka.actor.{ActorRef, Actor}
import multilink.game.network.intranet.Gateway.{Connected, Connect, Routed, Route}

class MockGateway(connectionEndpoint: ActorRef) extends Actor {
  override def receive: Receive = {
    case Route(from, through, to) =>
      sender ! Routed(from, through, to)
    case Connect(from, to) =>
      sender ! Connected(from, to)
  }
}
