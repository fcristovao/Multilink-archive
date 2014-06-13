package multilink.game.systems.internic

import akka.actor.Props
import multilink.util.composition.ComposableActor
import multilink.game.client.Client
import multilink.game.systems.internic.InterNIC.WelcomeToInterNIC

object InterNICWebServer {
  def apply() = Props[InterNICWebServer];
}

class InterNICWebServer extends ComposableActor{
  override def react: Receive = {
    case Client.Hello => sender() ! WelcomeToInterNIC
  }
}