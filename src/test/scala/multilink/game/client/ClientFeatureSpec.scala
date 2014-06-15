package multilink.game.client

import akka.testkit.TestProbe
import multilink.game.network.internet._
import akka.actor.{ActorRef, Props, ActorSystem}
import multilink.network.intranet.MockGateway
import multilink.game.client.Client._
import multilink.game.client.Client.Connected
import multilink.game.client.Client.Connect
import multilink.game.systems.internic.InterNIC
import multilink.util.testing.MultilinkTestFeatureSpec

class ClientFeatureSpec extends MultilinkTestFeatureSpec {

  feature("Client can use the InterNIC Internet Points address database") {
    ignore("Client can connect to the InterNIC") {
      Given("A Multilink Client")
      val mockConnectionEndpoint = TestProbe()
      val (client, _, _) = setupClient(mockRoute(mockConnectionEndpoint.ref, 1, 2))
      client ! Subscribe
      expectMsg(Subscribed)

      When("the client connects to the InterNIC")
      // Todo: Make the InterNIC address non-static
      client ! Connect(1, 2)
      expectMsg(Connected(2))

      Then("InterNIC must send it's greetings message")
      expectMsg(NiceToMeetYouMyNameIs(InterNIC))
    }
  }

  def mockRoute(connectionEndpoint: ActorRef, ips: InternetPointAddress*)(implicit system: ActorSystem) = {
    val mockGateway = system.actorOf(Props(classOf[MockGateway]))
    val ipsAndGateways =
      for (ip <- ips.dropRight(1)) yield {
        (ip, mockGateway)
      }
    ipsAndGateways :+(ips.last, connectionEndpoint)
  }

  def setupClient(ipdb: Seq[(InternetPointAddress, ActorRef)])(implicit system: ActorSystem) = {
    val mockIPDb = system.actorOf(
      Props(
        classOf[MockInternetPointsDatabase],
        scala.collection.mutable.Map(ipdb: _*)
      )
    )
    val dialer = system.actorOf(Props(classOf[Dialer], mockIPDb))
    val client = system.actorOf(Props(classOf[Client], mockIPDb, dialer))
    (client, dialer, mockIPDb)
  }
}
