package multilink.util.composition.channels

import akka.actor.{Props, ActorRefFactory, ActorRef, Actor}
import multilink.util.composition.{Composition, Lift, ArrowOperator, Composable}
import multilink.util.composition.channels.ChannelManager.{ChannelOpened, OpenChannel}

object NetworkGraph {


  class NetworkNode(val actor: ActorRef, var next: List[NetworkNode] = Nil) {
    override def toString = "NetworkNode"
  }

  def fromArrowOperator[A <: Actor with Composable](network: ArrowOperator[A])(implicit actorRefFactory: ActorRefFactory): NetworkNode = {
    network match {
      case Lift(actorProps, inbound, outbound) => {
        val actorRef = actorRefFactory.actorOf(actorProps)
        new NetworkNode(actorRef)
      }
      case Composition(composables) => {
        composables
        .map(fromArrowOperator(_))
        .reduceRight(
            (x, y) => {
              x.next = List(y)
              x
            }
          )
      }
    }
  }

}

object ChannelManager {
  sealed trait Messages
  case object OpenChannel extends Messages
  case class ChannelOpened(channel: ActorRef) extends Messages
}

class ChannelManager[A <: Actor with Composable](network: ArrowOperator[A]) extends Actor {
  val rootCompositionNetwork = NetworkGraph.fromArrowOperator(network)
  private val channelIds = Iterator from 1

  def receive = {
    case OpenChannel => {
      val channel = createChannel()
      sender() ! ChannelOpened(channel)
    }
    case msg => {
      val channel = createChannel()
      channel.forward(msg)
    }

  }

  def createChannel(): ActorRef = {
    context.actorOf(Props(classOf[Channel], channelIds.next(), rootCompositionNetwork))
  }
}
