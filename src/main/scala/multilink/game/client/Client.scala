package multilink.game.client

import akka.actor._
import akka.actor.FSM.SubscribeTransitionCallBack

object Client {
  sealed trait Messages
  case object Subscribe extends Messages
  case object Unsubscribe extends Messages
  case object Subscribed extends Messages
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