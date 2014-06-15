package multilink.util.testing

import akka.testkit.{ImplicitSender, TestKit}
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import org.scalatest.{GivenWhenThen, FeatureSpecLike, BeforeAndAfterAll, WordSpecLike}

class MultilinkTestKit extends TestKit(ActorSystem("test", ConfigFactory.load("application-test")))
                               with ImplicitSender {
}

class MultilinkTestWordSpec extends MultilinkTestKit with WordSpecLike with BeforeAndAfterAll {
  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }
}

class MultilinkTestFeatureSpec extends MultilinkTestKit with FeatureSpecLike with BeforeAndAfterAll with GivenWhenThen {
  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }
} 
