package multilink.game.client

import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import multilink.game.client.Dialer.{UnknownInternetPoint, IllegalRoute}

class DialerSpec extends TestKit(ActorSystem("test", ConfigFactory.load("application-test")))
                         with WordSpecLike with ImplicitSender with BeforeAndAfterAll {

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  "A Dialer" should {
    "not accept routing lists with less than 2 points" in {
      val dialer = system.actorOf(Props(classOf[Dialer], TestProbe().ref))
      dialer ! Dialer.CreateRoute(List(1))
      expectMsg(Dialer.RouteFailed(IllegalRoute(List(1))))
    }
    "fail to create a route through an Internet Point if it doesn't exist" in {
      val mockIPDb = TestProbe()
      val mockOurGateway = TestProbe()
      val dialer = system.actorOf(Props(classOf[Dialer], mockIPDb.ref))
      dialer ! Dialer.CreateRoute(List(1, 2))
      mockIPDb.expectMsg(InternetPointsDatabase.Get(1))
      mockIPDb.reply(InternetPointsDatabase.IPFor(1, Some(mockOurGateway.ref)))
      mockIPDb.expectMsg(InternetPointsDatabase.Get(2))
      mockIPDb.reply(InternetPointsDatabase.IPFor(2, None))
      expectMsg(Dialer.RouteFailed(UnknownInternetPoint(2)))
    }
    "be to create a route directly to an Internet Point if it exists" in {
      val mockIPDb = TestProbe()
      val mockGateway = TestProbe()
      val dialer = system.actorOf(Props(classOf[Dialer], mockIPDb.ref))
      dialer ! Dialer.CreateRoute(List(1, 2))
      mockIPDb.expectMsg(InternetPointsDatabase.Get(1))
      mockIPDb.reply(InternetPointsDatabase.IPFor(1, Some(mockGateway.ref)))
      mockIPDb.expectMsg(InternetPointsDatabase.Get(2))
      mockIPDb.reply(InternetPointsDatabase.IPFor(2, Some(mockGateway.ref)))
      expectMsgType[Dialer.RouteCreated]
    }

  }
}
