package multilink.game.client

import akka.testkit.TestProbe
import akka.actor.Props
import multilink.game.client.Dialer.{UnknownInternetPoint, IllegalRoute}
import multilink.util.testing.MultilinkTestWordSpec

class DialerSpec extends MultilinkTestWordSpec {

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
