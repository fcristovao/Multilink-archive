package multilink.game.client

import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import akka.actor.{ActorRef, Props, ActorSystem}
import com.typesafe.config.ConfigFactory
import multilink.game.client.Client.{Subscribed, Subscribe}
import multilink.network.intranet.MockGateway
import multilink.game.network.internet._
import akka.actor.FSM.Transition
import akka.actor.FSM.CurrentState

class ClientSpec extends TestKit(ActorSystem("test", ConfigFactory.load("application-test")))
                         with WordSpecLike with ImplicitSender with BeforeAndAfterAll {

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

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
      val (client, _, _) = testSetup(mockRoute(mockConnectionEndpoint, 1, 2))

      client ! Subscribe
      expectMsg(Subscribed)

    }

  }
  "Multilink Client's Subscribers" should {
    "get updates of the Dialer's State as one route is being established" in {
      val mockConnectionEndpoint = TestProbe()
      val (client, dialer, _) = testSetup(mockRoute(mockConnectionEndpoint, 1, 2))

      client ! Subscribe
      expectMsgAllOf(Subscribed, CurrentState(dialer, Dialer.Idle))

      client ! Dialer.CreateRoute(List(1, 2))
      expectMsgAllOf(
        Transition(dialer, Dialer.Idle, Dialer.LocatingInternetPoints),
        Transition(dialer, Dialer.LocatingInternetPoints, Dialer.Idle)
      )
    }
  }

  def mockRoute(connectionEndpoint: TestProbe, ips: InternetPointAddress*)(implicit system: ActorSystem) = {
    val mockGateway = system.actorOf(Props(classOf[MockGateway], connectionEndpoint.ref))
    val ipsAndGateways =
      for (ip <- ips) yield {
        (ip, mockGateway)
      }
    ipsAndGateways
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