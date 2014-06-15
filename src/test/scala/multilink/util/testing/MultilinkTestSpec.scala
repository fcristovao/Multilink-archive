package multilink.util.testing

import akka.testkit.{ImplicitSender, TestKit}
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class MultilinkTestSpec extends TestKit(ActorSystem("test", ConfigFactory.load("application-test")))
                                with WordSpecLike with ImplicitSender with BeforeAndAfterAll {

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }
}
