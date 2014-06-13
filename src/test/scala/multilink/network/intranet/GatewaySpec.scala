package multilink.network.intranet

import akka.testkit.{ImplicitSender, TestKit}
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import multilink.util.composition._
import multilink.game.network.intranet.Gateway

class GatewaySpec extends TestKit(ActorSystem("test", ConfigFactory.load("application-test")))
                          with WordSpecLike with ImplicitSender with BeforeAndAfterAll {

  val fromInternetPointAddress = 1
  val internetPointAddress = 2
  val otherInternetPointAddress = 3

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  "A gateway" should {
    "accept connections if the InternetPointAddress matches his own" in {
      val channel = openChannelFor(Gateway(internetPointAddress))
      channel ! Gateway.Connect(fromInternetPointAddress, internetPointAddress)
      expectMsg(Gateway.Connected(fromInternetPointAddress, internetPointAddress))
    }
    "reject connections if the InternetPointAddress doesn't match his own" in {
      val channel = openChannelFor(Gateway(internetPointAddress))
      channel ! Gateway.Connect(fromInternetPointAddress, otherInternetPointAddress)
      expectMsg(Gateway.ConnectionRefused)
    }
    "route connections if the InternetPointAddress matches his own" in {
      val channel = openChannelFor(Gateway(internetPointAddress))
      channel ! Gateway.Route(fromInternetPointAddress, internetPointAddress, otherInternetPointAddress)
      expectMsg(Gateway.Routed(fromInternetPointAddress, internetPointAddress, otherInternetPointAddress))
    }
    "reject routing if the InternetPointAddress doesn't match his own" in {
      val channel = openChannelFor(Gateway(internetPointAddress))
      channel ! Gateway.Route(fromInternetPointAddress, otherInternetPointAddress, otherInternetPointAddress)
      expectMsg(Gateway.ConnectionRefused)
    }
  }

}
