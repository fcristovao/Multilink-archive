package multilink.game.client

import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import akka.testkit._
import akka.actor.{ActorInitializationException, ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import multilink.game.network.internet.InternetPointAddress
import multilink.game.client.Route._
import multilink.game.network.intranet.Gateway

class RouteSpec extends TestKit(ActorSystem("test", ConfigFactory.load("application-test")))
                         with WordSpecLike with ImplicitSender with BeforeAndAfterAll {

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  "A Route" should {
    "not accept routing lists with less than 2 points" in {
      val gateways = mockGateways(1)
      EventFilter[ActorInitializationException](occurrences = 1) intercept {
        TestActorRef(Props(classOf[Route], gateways))
      }
    }
    "fail to connect directly to an Internet Point if the gateway fails to answer in time" in {

    }
    "be able to connect directly to an Internet Point if the gateway answers" in {
      val probes = mockGateways(1, 2)
      val gateways = Map(probes: _*)
      val route = system.actorOf(Props(classOf[Route], probes.map({case (x, y:TestProbe) => (x, y.ref)})))
      route ! Route.Connect
      gateways(2).expectMsg(Gateway.Connect(1,2))
      gateways(2).reply(Gateway.Connected(1,2))
      expectMsg(Route.ConnectionEstablished())
    }

  }

  def mockGateways(ips : InternetPointAddress*)(implicit system: ActorSystem) = {
    val ipsAndProbes =
      for (ip <- ips;
           testProbe = TestProbe())
      yield (ip, testProbe)
    ipsAndProbes
  }
}
