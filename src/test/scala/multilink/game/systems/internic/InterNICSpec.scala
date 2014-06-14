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

  implicit val config = InterNIC.Config(2, Map("www.google.com" -> 4, "akka.io" -> 8), Map("admin" -> "12345"))

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  "An InterNIC system" should {
    "accept connections" in {
      val channel = openChannelFor(InterNIC(config))
      channel ! Gateway.Connect(1,config.ip)
      expectMsg(Gateway.Connected(1,config.ip))
    }
    "answer to Client.Hello messages with Welcome message" in {
      val channel = openChannelFor(InterNIC(config))
      channel ! Client.Hello
      expectMsg(Client.NiceToMeetYouMyNameIs(InterNIC))
    }
    "answer with the global Address book when requested" in {
      val channel = openChannelFor(InterNIC(config))
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


