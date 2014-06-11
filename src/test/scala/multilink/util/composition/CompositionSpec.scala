package multilink.util.composition

import akka.testkit.TestKit
import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.ImplicitSender
import org.scalatest.{WordSpecLike, BeforeAndAfterAll}
import scala.collection.mutable.Queue
import com.typesafe.config.ConfigFactory
import multilink.util.composition.channels.ChannelManager.{ChannelOpened, OpenChannel}

object CompositionSpec {

  sealed trait Messages
  case object PassThrough extends Messages
  case class UniquePassThrough(id: Int) extends Messages
  case object GetReplyFromAll extends Messages
  case class CountFrom(id: Int, count: Int) extends Messages
  case class ReplyFrom(id: Int) extends Messages
  case class GetReplyFrom(id: Int) extends Messages
  case class GetCountFrom(id: Int) extends Messages
  case object Discharge extends Messages

  class Base(id: Int) extends ComposableActor {
    val counter = Iterator from 1
    var soFar = 0
    val accumulator: Queue[Messages] = Queue()

    def react = {
      case Discharge => accumulator.foreach(msg => sender ! msg)
      case GetCountFrom(expectedId) if expectedId == id => sender ! CountFrom(id, soFar)
      case GetReplyFrom(expectedId) if expectedId == id => sender ! ReplyFrom(id)
      case msg: Messages => {
        soFar = counter.next()
        accumulator.enqueue(msg)
      }
    }
  }

  case class Replier(id: Int) extends Base(id) {
    override def react = {
      case GetReplyFromAll => {
        sender ! ReplyFrom(id)
      }
      case anythingElse => super.react(anythingElse)
    }
  }

  case class Pass(id: Int) extends Base(id) {
    override def react = {
      case anythingElse => super.react(anythingElse)
    }
  }

}


class CompositionSpec extends TestKit(ActorSystem("TestSystem", ConfigFactory.load("application-test")))
                              with ImplicitSender with WordSpecLike with BeforeAndAfterAll {
  import CompositionSpec._
  import ArrowOperator._

  override def afterAll() {
    system.shutdown()
  }

  "A composition network" should {
    "be able to create a channel to it" in {
      openChannelFor(Pass(1))
    }
    "allow you to message the network without a channel" in {
      val network = Pass(1).lift
      val networkActor = system.actorOf(network)

      networkActor ! GetReplyFrom(1)
      expectMsg(ReplyFrom(1))
    }
    "allow you to message the network with a channel" in {
      val channel = openChannelFor(Pass(1))
      channel ! GetReplyFrom(1)
      expectMsg(ReplyFrom(1))
    }
    "reach every actor only if none replies in a composition (1)" in {
      val channel = openChannelFor(Pass(1) >>> Pass(2))
      channel ! PassThrough
      channel ! GetCountFrom(1)
      expectMsg(CountFrom(1, 1))
      channel ! GetCountFrom(2)
      expectMsg(CountFrom(2, 1))
      channel ! GetCountFrom(1)
      expectMsg(CountFrom(1, 2)) // It now has the one it just let through to the second
    }

  }
  "N messages sent into a composition network" should {
    "reach every actor if none replies" in {
      val pass1 = Pass(1).lift
      val pass2 = Pass(2).lift
      val pass3 = Pass(3).lift
      val pass4 = Pass(4).lift
      val network = pass1 >>> pass2 >>> pass3 >>> pass4
      val channel = openChannelFor(network)

      val limit = 1000

      val uniqueMsgs = for (i <- 1 to limit)
      yield {
        UniquePassThrough(i)
      }

      uniqueMsgs.foreach(msg => channel ! msg)

      channel ! GetReplyFrom(4)
      expectMsg(ReplyFrom(4))

      channel ! GetCountFrom(1)
      expectMsg(CountFrom(1, limit + 1))
      channel ! GetCountFrom(2)
      expectMsg(CountFrom(2, limit + 1))
      channel ! GetCountFrom(3)
      expectMsg(CountFrom(3, limit + 1))
      channel ! GetCountFrom(4)
      expectMsg(CountFrom(4, limit))
    }
    /*
          "reach every actor if none replies in the same order they were sent" in {
            val pass1 = Pass(1).lift
            val pass2 = Pass(2).lift
            val pass3 = Pass(3).lift
            val pass4 = Pass(4).lift
            val network = pass1 >>> pass2 >>> pass3 >>> pass4
            val networkActor = system.actorOf(Props(network))

            val limit = 10000

            val uniqueMsgs = for(i<- 0 to limit)
            yield UniquePassThrough(i)

            uniqueMsgs.foreach(msg => networkActor ! msg)

            networkActor ! GetReplyFrom(1)
            expectMsg(ReplyFrom(1))

            pass1 ! Discharge
            pass1 ! GetReplyFrom(1)
            uniqueMsgs.foreach(expectMsg(_))
            expectMsg(ReplyFrom(1))

            pass2 ! Discharge
            pass2 ! GetReplyFrom(2)
            uniqueMsgs.foreach(expectMsg(_))
            expectMsg(ReplyFrom(2))

            pass3 ! Discharge
            pass3 ! GetReplyFrom(3)
            uniqueMsgs.foreach(expectMsg(_))
            expectMsg(ReplyFrom(3))

            pass4 ! Discharge
            pass4 ! GetReplyFrom(4)
            uniqueMsgs.foreach(expectMsg(_))
            expectMsg(ReplyFrom(4))
          }

          "generate replies from every actor that responds" in {
            val network = Pass(1) >>> Pass(2) >>> Pass(3) >>> (Replier(4) &&& Replier(5) &&& Replier(6))
            val networkActor = system.actorOf(Props(network))

            networkActor ! GetReplyFromAll
            expectMsgAllOf(ReplyFrom(4),ReplyFrom(5),ReplyFrom(6))
          }
          */
  }

  def openChannelFor(network: ArrowOperator[_]): ActorRef = {
    val networkActor = system.actorOf(network)

    networkActor ! OpenChannel
    val ChannelOpened(channel) = expectMsgType[ChannelOpened]
    channel
  }
}
