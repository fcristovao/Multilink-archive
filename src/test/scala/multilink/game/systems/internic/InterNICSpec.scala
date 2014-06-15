package multilink.game.systems.internic

import multilink.game.network.intranet.{Logger, AccessControl, Gateway}
import multilink.util.composition._
import multilink.game.client.Client
import multilink.util.testing.MultilinkTestWordSpec

class InterNICSpec extends MultilinkTestWordSpec {

  implicit val config = InterNIC.Config(2, Map("www.google.com" -> 4, "akka.io" -> 8), Map("admin" -> "12345"))

  "An InterNIC system" should {
    "accept connections" in {
      val channel = openChannelFor(InterNIC(config))
      channel ! Gateway.Connect(1, config.ip)
      expectMsg(Gateway.Connected(1, config.ip))
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
    "should allow you to login" in {
      val channel = openChannelFor(InterNIC(config))
      channel ! AccessControl.Login("admin", "12345")
      expectMsg(AccessControl.AccessGranted("admin"))
    }
    "should allow you to access the system logs after login was done" ignore {
      val channel = openChannelFor(InterNIC(config))
      channel ! AccessControl.Login("admin", "12345")
      expectMsg(AccessControl.AccessGranted("admin"))
      channel ! Logger.GetLogs
      expectMsgType[Logger.Logs]
    }
  }

}


