package multilink.game.systems.internic

import akka.actor.Props
import multilink.util.composition.ComposableActor
import multilink.game.client.Client
import scala.collection.immutable
import multilink.game.network.internet._

object InterNICWebServer {
  def apply(addressBook: Map[String, InternetPointAddress]) = Props(classOf[InterNICWebServer], addressBook)

  sealed trait Messages
  case object GetIPAddressBook extends Messages
  case class IPAddressBook(addresses: immutable.Map[String, InternetPointAddress])
}

class InterNICWebServer(val addressBook: Map[String, InternetPointAddress]) extends ComposableActor {
  import InterNICWebServer._

  override def react: Receive = {
    case Client.Hello => sender() ! Client.NiceToMeetYouMyNameIs(InterNIC)
    case GetIPAddressBook => sender() ! IPAddressBook(addressBook)
  }
}