package multilink.game.systems.internic

import multilink.game.network.intranet.Gateway
import multilink.game.network.internet._

object InterNIC {
  import multilink.util.composition.ArrowOperator._

  def apply(ip: InternetPointAddress) = {
    Gateway(ip) >>> InterNICWebServer(Map("www.google.com" -> 4, "akka.io" -> 8))
  }
}

