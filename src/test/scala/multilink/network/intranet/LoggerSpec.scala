package multilink.network.intranet

import multilink.util.testing.MultilinkTestSpec
import multilink.util.composition._
import multilink.game.network.intranet.Logger
import multilink.game.network.intranet.Logger.{Logs, GetLogs}

class LoggerSpec extends MultilinkTestSpec {

  "A Logger" should {
    "log the messages that go through it and return them when asked" in {
      val channel = openChannelFor(Logger())
      channel ! "LoggedMessage" //for now, in the future the logger will only log specific messages
      channel ! GetLogs
      expectMsg(Logs(List("LoggedMessage")))
    }
  }
}