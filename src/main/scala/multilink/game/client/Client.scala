package multilink.game.client

import akka.actor._
import akka.actor.FSM.SubscribeTransitionCallBack
import multilink.game.network.internet.InternetPointAddress

object Client {
  sealed trait Messages
  // Subscription
  case object Subscribe extends Messages
  case object Subscribed extends Messages
  // Connections
  case class Connect(internetPointAddresses: InternetPointAddress*) extends Messages
  case class Connected(internetPointAddress: InternetPointAddress) extends Messages
  // Interactions
  case object Hello extends Messages
  case class NiceToMeetYouMyNameIs(identity: Any) extends Messages
}

class Client(ipdb: ActorRef, dialer: ActorRef) extends Actor {
  import Client._

  def receive = {
    case Subscribe => {
      sender() ! Subscribed
      dialer ! SubscribeTransitionCallBack(sender())
    }
    case msg: Dialer.Messages => dialer ! msg
  }
}