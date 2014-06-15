package multilink.game.client

import akka.testkit.TestProbe
import akka.actor.{ActorRef, Props, ActorSystem}
import multilink.game.client.Client.{Subscribed, Subscribe}
import multilink.network.intranet.MockGateway
import multilink.game.network.internet._
import akka.actor.FSM.Transition
import akka.actor.FSM.CurrentState
import multilink.util.testing.MultilinkTestWordSpec

class ClientSpec extends MultilinkTestWordSpec {

  "A Multilink client" should {
    "accept subscribers" in {
      val mockInternetPointsDatabase = TestProbe()
      val mockDialer = TestProbe()
      val client = system.actorOf(Props(classOf[Client], mockInternetPointsDatabase.ref, mockDialer.ref))

      client ! Subscribe
      expectMsg(Subscribed)
    }
    "expose the current state of its components when subscriptions occur" in {
      val mockIPDb = TestProbe()
      val dialer = system.actorOf(Props(classOf[Dialer], mockIPDb.ref))
      val client = system.actorOf(Props(classOf[Client], mockIPDb.ref, dialer))

      client ! Subscribe
      expectMsgAllOf(Subscribed, CurrentState(dialer, Dialer.Idle))
    }
    "be able to connect directly to an Internet Point" in {
      val mockConnectionEndpoint = TestProbe()
      val (client, _, _) = testSetup(mockRoute(mockConnectionEndpoint.ref, 1, 2))

      client ! Subscribe
      expectMsg(Subscribed)

    }
  }

  "Multilink Client's Subscribers" should {
    "get updates of the Dialer's State as one route is being established" in {
      val mockConnectionEndpoint = TestProbe()
      val (client, dialer, _) = testSetup(mockRoute(mockConnectionEndpoint.ref, 1, 2))

      client ! Subscribe
      expectMsgAllOf(Subscribed, CurrentState(dialer, Dialer.Idle))

      client ! Dialer.CreateRoute(List(1, 2))
      expectMsgAllOf(
        Transition(dialer, Dialer.Idle, Dialer.LocatingInternetPoints),
        Transition(dialer, Dialer.LocatingInternetPoints, Dialer.Idle)
      )
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

  def testSetup(ipdb: Seq[(InternetPointAddress, ActorRef)])(implicit system: ActorSystem) = {
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