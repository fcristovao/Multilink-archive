package multilink.game.systems.internic

import multilink.game.network.intranet.Gateway
import multilink.game.network.internet._
import scala.collection.immutable

object InterNIC {
  import multilink.util.composition.ArrowOperator._

  sealed trait Messages
  case object GetIPAddressBook extends Messages
  case class IPAddressBook(addresses: immutable.Map[String, InternetPointAddress])

  def apply(ip: InternetPointAddress) = {
    Gateway(ip) >>> InterNICWebServer()
  }
}

