package multilink.util

import akka.actor.{ActorRefFactory, ActorRef}
import multilink.util.composition.channels.ChannelManager.OpenChannel
import akka.pattern.ask
import multilink.util.composition.channels.ChannelManager.ChannelOpened
import scala.concurrent.duration._
import scala.concurrent.Await
import akka.util.Timeout

package object composition {

  def openChannelFor(network: ArrowOperator[_])(implicit actorFactory: ActorRefFactory): ActorRef = {
    openChannelFor(actorFactory.actorOf(network))
  }

  def openChannelFor(networkActor: ActorRef): ActorRef = {
    implicit val timeout = Timeout(1 seconds)
    val ChannelOpened(channel) = Await.result((networkActor ? OpenChannel).mapTo[ChannelOpened], timeout.duration)
    channel
  }

}
