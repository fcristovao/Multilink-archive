package multilink.game.systems.internic

import akka.actor.Props
import multilink.util.composition.ComposableActor
import multilink.game.client.Client
import multilink.game.network.internet._
import multilink.game.systems.internic.InterNICWebServer.AddressBook
import scala.collection.immutable

object InterNICWebServer {

  type AddressBook = immutable.Map[String, InternetPointAddress]

  def apply(addressBook: AddressBook) = Props(classOf[InterNICWebServer], addressBook)

  sealed trait Messages
  case object GetIPAddressBook extends Messages
  case class IPAddressBook(addresses: AddressBook)
}

class InterNICWebServer(val addressBook: AddressBook) extends ComposableActor {
  import InterNICWebServer._

  override def react: Receive = {
    case Client.Hello => sender() ! Client.NiceToMeetYouMyNameIs(InterNIC)
    case GetIPAddressBook => sender() ! IPAddressBook(addressBook)
  }
}