package multilink.game.client

import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import akka.testkit.{TestProbe, ImplicitSender, TestKit}
import akka.actor.{Props, ActorSystem}
import com.typesafe.config.ConfigFactory

class ClientSpec extends TestKit(ActorSystem("test", ConfigFactory.load("application-test")))
                         with WordSpecLike with ImplicitSender with BeforeAndAfterAll {

  val mockInternetPointsDatabase = TestProbe()
  val mockDialer = TestProbe()

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  "A Multilink client" should {
    "be able to connect to an Internet Point" in {
      //system.actorOf(Props(classOf[Client], mockInternetPointsDatabase.ref, mockDialer.ref))
    }
  }
}
