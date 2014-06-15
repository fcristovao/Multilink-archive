package multilink.network.intranet

import multilink.util.composition._
import multilink.game.network.intranet.Gateway
import multilink.util.testing.MultilinkTestWordSpec

class GatewaySpec extends MultilinkTestWordSpec {

  val fromInternetPointAddress = 1
  val internetPointAddress = 2
  val otherInternetPointAddress = 3

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
