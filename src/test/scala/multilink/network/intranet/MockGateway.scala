package multilink.network.intranet

import akka.actor.Actor
import multilink.game.network.intranet.Gateway.{Connected, Connect, Routed, Route}

class MockGateway extends Actor {
  override def receive: Receive = {
    case Route(from, through, to) =>
      sender ! Routed(from, through, to)
    case Connect(from, to) =>
      sender ! Connected(from, to)
  }
}
