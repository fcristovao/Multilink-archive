package multilink.game.systems.internic

import akka.actor.Props
import multilink.util.composition.ComposableActor
import multilink.game.client.Client
import multilink.game.systems.internic.InterNIC.{IPAddressBook, GetIPAddressBook}

object InterNICWebServer {
  def apply() = Props[InterNICWebServer];
}

class InterNICWebServer extends ComposableActor {
  val addressBook = Map("www.google.com" -> 4, "akka.io" -> 8)

  override def react: Receive = {
    case Client.Hello => sender() ! Client.NiceToMeetYouMyNameIs(InterNIC)
    case GetIPAddressBook => sender() ! IPAddressBook(addressBook)
  }
}