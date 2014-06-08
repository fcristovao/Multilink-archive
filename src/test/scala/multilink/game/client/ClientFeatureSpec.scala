package multilink.game.client

import org.scalatest.{FeatureSpecLike, BeforeAndAfterAll, GivenWhenThen}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import multilink.game.network.internet._
import akka.actor.{ActorRef, Props, ActorSystem}
import multilink.network.intranet.MockGateway
import multilink.game.client.Client.{Connected, Subscribed, Subscribe, Connect}
import com.typesafe.config.ConfigFactory
import multilink.game.systems.InterNIC.WelcomeToInterNIC

class ClientFeatureSpec extends TestKit(ActorSystem("test", ConfigFactory.load("application-test")))
                                with ImplicitSender with BeforeAndAfterAll with FeatureSpecLike with GivenWhenThen {

  feature("Client can use the InterNIC Internet Points address database") {
    scenario("Client can connect to the InterNIC") {
      Given("A Multilink Client")
      val mockConnectionEndpoint = TestProbe()
      val (client, _, _) = setupClient(mockRoute(mockConnectionEndpoint, 1, 2))
      client ! Subscribe
      expectMsg(Subscribed)

      When("the client connects to the InterNIC")
      // Todo: Make the InterNIC address non-static
      client ! Connect(1, 2)
      expectMsg(Connected(2))

      Then("InterNIC must send it's greetings message")
      expectMsg(WelcomeToInterNIC)
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
