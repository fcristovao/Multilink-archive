package multilink.game.systems

import akka.testkit.{ImplicitSender, TestKit}
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import multilink.game.network.intranet.Gateway
import multilink.util.composition._

class InterNicSpec extends TestKit(ActorSystem("test", ConfigFactory.load("application-test")))
                           with WordSpecLike with ImplicitSender with BeforeAndAfterAll {

  val InterNICInternetPoint = 2

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  "An InterNIC system" should {
    "accept connections" in {
      val channel = openChannelFor(InterNIC(InterNICInternetPoint))
      channel ! Gateway.Connect(1,InterNICInternetPoint)
      expectMsg(Gateway.Connected(1,InterNICInternetPoint))
    }

  }

}


