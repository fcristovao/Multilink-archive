package multilink.game.client

import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import akka.testkit.{EventFilter, TestProbe, ImplicitSender, TestKit}
import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import multilink.game.network.intranet.Gateway

class DialerSpec extends TestKit(ActorSystem("test", ConfigFactory.load("application-test")))
                         with WordSpecLike with ImplicitSender with BeforeAndAfterAll {

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  "A Dialer" should {
    "not accept routing lists with less than 2 points" in {
      val dialer = system.actorOf(Props(classOf[Dialer], TestProbe().ref))
      EventFilter[IllegalArgumentException](occurrences = 1) intercept {
        dialer ! Dialer.Connect(List(1))
      }
    }
    "be able to connect directly to an Internet Point if it exists" in {
      val mockIPDb = TestProbe()
      val mockGateway = TestProbe()
      val dialer = system.actorOf(Props(classOf[Dialer], mockIPDb.ref))
      dialer ! Dialer.Connect(List(1,2))
      mockIPDb.expectMsg(InternetPointsDatabase.Get(2))
      mockIPDb.reply(InternetPointsDatabase.IPFor(2, Some(mockGateway.ref)))
      mockGateway.expectMsg(Gateway.Connect(1,2))
      mockGateway.reply(Gateway.Connected(1,2))
      expectMsg(Dialer.ConnectionEstablished)
    }
    "fail to connect directly to an Internet Point if it doesn't exist" in {
      val mockIPDb = TestProbe()
      val dialer = system.actorOf(Props(classOf[Dialer], mockIPDb.ref))
      dialer ! Dialer.Connect(List(1,2))
      mockIPDb.expectMsg(InternetPointsDatabase.Get(2))
      mockIPDb.reply(InternetPointsDatabase.IPFor(2, None))
      expectMsg(Dialer.ConnectionFailed(2))
    }

  }
}
