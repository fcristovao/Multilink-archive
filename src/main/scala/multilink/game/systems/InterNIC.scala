package multilink.game.systems

import akka.actor.Props
import multilink.game.network.intranet.Gateway
import multilink.game.network.internet._

object InterNIC {
  sealed trait Messages
  object WelcomeToInterNIC extends Messages

  def apply(ip: InternetPointAddress): Props = {
    //Gateway() >>> InterNICWebServer()
    Gateway(ip)
  }
}

class InterNIC {

}
