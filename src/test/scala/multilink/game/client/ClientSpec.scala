package multilink.game.client

import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import akka.actor.{Props, ActorSystem}
import com.typesafe.config.ConfigFactory
import multilink.game.client.Client.{Subscribed, Subscribe}
import akka.actor.FSM.{Transition, CurrentState}
import multilink.network.intranet.MockGateway

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
  }
  "Multilink Client's Subscribers" should {
    val mockGateway = system.actorOf(Props[MockGateway])
    val mockIPDb = system.actorOf(
      Props(
        classOf[MockInternetPointsDatabase],
        scala.collection.mutable.Map(2 -> mockGateway)
      )
    )

    "get updates of the Dialer's State as one route is being established" in {
      val dialer = system.actorOf(Props(classOf[Dialer], mockIPDb))
      val client = system.actorOf(Props(classOf[Client], mockIPDb, dialer))

      client ! Subscribe
      expectMsgAllOf(Subscribed, CurrentState(dialer, Dialer.Idle))

      client ! Dialer.CreateRoute(List(1, 2))
      expectMsgAllOf(
        Transition(dialer, Dialer.Idle, Dialer.LocatingInternetPoints),
        Transition(dialer, Dialer.LocatingInternetPoints, Dialer.Idle)
      )
    }
  }
}
