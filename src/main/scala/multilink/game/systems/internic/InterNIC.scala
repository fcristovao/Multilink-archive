package multilink.game.systems.internic

import akka.actor.Props
import multilink.game.network.intranet.Gateway
import multilink.game.network.internet._

object InterNIC {
  import multilink.util.composition.ArrowOperator._

  sealed trait Messages
  object WelcomeToInterNIC extends Messages

  def apply(ip: InternetPointAddress): Props = {
    Gateway(ip) >>> InterNICWebServer()
  }
}

