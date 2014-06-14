package multilink.game.systems.internic

import akka.testkit.{ImplicitSender, TestKit}
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import multilink.game.network.intranet.Gateway
import multilink.util.composition._
import multilink.game.client.Client

class InterNICSpec extends TestKit(ActorSystem("test", ConfigFactory.load("application-test")))
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
    "answer to Client.Hello messages with Welcome message" in {
      val channel = openChannelFor(InterNIC(InterNICInternetPoint))
      channel ! Client.Hello
      expectMsg(Client.NiceToMeetYouMyNameIs(InterNIC))
    }
    "answer with the global Address book when requested" in {
      val channel = openChannelFor(InterNIC(InterNICInternetPoint))
      channel ! InterNICWebServer.GetIPAddressBook
      expectMsgType[InterNICWebServer.IPAddressBook]
    }
    /*
    "should allow you to login" in {
      val channel = openChannelFor(InterNIC(InterNICInternetPoint))
      channel ! AccessControl.Login()
      expectMsgType[InterNIC.IPAddressBook]
    }
    */
  }

}


