package multilink.network.intranet

import scala.collection.immutable
import multilink.game.network.intranet.AccessControl.{AccessDenied, Login, AccessGranted}
import multilink.game.network.intranet.AccessControl
import multilink.util.composition._
import ArrowOperator._
import multilink.util.testing.MultilinkTestWordSpec

class AccessControlSpec extends MultilinkTestWordSpec {

  case class Hello() extends ComposableActor {
    def react = {
      case "Hello" => sender() ! "HelloBack"
    }
  }

  val defaultCredentials = immutable.Map(
    "admin" -> "12345",
    "anotherUser" -> "54321"
  )

  "An Access Control" should {
    "deny access for an unknown user" in {
      val channel = openChannelFor(AccessControl(defaultCredentials) >>> Hello())
      channel ! Login("qwerty", "12345")
      expectMsg(AccessDenied)
    }
    "deny access for an known user but incorrect password" in {
      val channel = openChannelFor(AccessControl(defaultCredentials) >>> Hello())
      channel ! Login("admin", "qwerty")
      expectMsg(AccessDenied)
    }
    "grant access if given the correct credentials" in {
      val channel = openChannelFor(AccessControl(defaultCredentials) >>> Hello())
      channel ! Login("admin", "12345")
      expectMsg(AccessGranted("admin"))
    }
    "deny access further in the network when no login was done" in {
      val channel = openChannelFor(AccessControl(defaultCredentials) >>> Hello())
      channel ! "Hello"
      expectMsg(AccessDenied)
      expectNoMsg()
    }
    "allow access further in the network after proper login was done" in {
      val channel = openChannelFor(AccessControl(defaultCredentials) >>> Hello())
      channel ! Login("admin", "12345")
      expectMsg(AccessGranted("admin"))
      channel ! "Hello"
      expectMsg("HelloBack")
    }
    "timeout logged in sessions after a configured duration" in {
      import scala.concurrent.duration._
      val config = AccessControl.Config(1 seconds)
      val channel = openChannelFor(AccessControl(defaultCredentials)(config) >>> Hello())
      channel ! Login("admin", "12345")
      expectMsg(AccessGranted("admin"))
      channel ! "Hello"
      expectMsg("HelloBack")
      within(config.sessionTimeout, config.sessionTimeout + (3 seconds)) {
        Thread.sleep((config.sessionTimeout + (1 seconds)).toMillis)
        channel ! "Hello"
        expectMsg(AccessDenied)
        expectNoMsg()
      }
    }
  }
}
