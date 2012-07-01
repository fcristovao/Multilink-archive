package multilink.util.composition

import akka.testkit.TestKit
import akka.actor.ActorSystem
import akka.testkit.ImplicitSender
import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfterAll
import multilink.util.MultilinkActor
import akka.actor.ActorRef
import akka.actor.Props
import org.scalatest.WordSpec
import scala.collection.mutable.Queue
import akka.actor.Actor

object CompositionSuite{
	
	sealed trait Messages
	case object PassThrough extends Messages
	case class UniquePassThrough(val id: Int) extends Messages
	case object GetReplyFromAll extends Messages
	case class ReplyFrom(id: Int) extends Messages
	case class GetReplyFrom(id: Int) extends Messages
	case object GetCount extends Messages
	case object Discharge extends Messages
	
	class Base(id: Int) extends MultilinkActor with ComposableActor {
		val counter = Iterator from 0
		var soFar = 0
		val accumulator: Queue[Messages] = Queue()
		
		def react = {
			case Discharge => accumulator.foreach(msg => sender ! msg)
			case GetCount => sender ! soFar
			case msg @ GetReplyFrom(expectedId) if expectedId==id => sender ! ReplyFrom(id)
			case msg : Messages => {
				soFar = counter.next()
				accumulator.enqueue(msg)
			}
		}
	}
	
	case class Replier(id: Int) extends Base(id) {
		override def react = {
			case msg @ GetReplyFromAll => {
				sender ! ReplyFrom(id)
				super.react(msg)
			}
		} 
	}

	case class Pass(id: Int) extends Base(id){
		override def react = {
			case anythingElse => super.react(anythingElse)
		} 
	}
	
}


class CompositionSuite extends TestKit(ActorSystem("TestSystem")) with ImplicitSender with WordSpec with BeforeAndAfterAll{
	import CompositionSuite._
	import Composable._
	import akka.util.duration._
	
	override def afterAll{
		system.shutdown()
	}
		
	"A composition network" should {
		"allow you to message the lifted actors" in {
			val network = Replier(1).lift
			val networkActor = system.actorOf(Props(network))
			
			networkActor ! GetReplyFromAll
			expectMsg(ReplyFrom(1))
			network ! GetReplyFromAll
			expectMsg(ReplyFrom(1))
		}
	}
	
	"N messages sent into a composition network" should {
		"reach every actor if none replies" in {
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
			
			networkActor ! GetReplyFrom(4)
			expectMsg(ReplyFrom(4))
			
			pass1 ! GetCount
			expectMsg(limit+1)
			pass2 ! GetCount
			expectMsg(limit+1)
			pass3 ! GetCount
			expectMsg(limit+1)
			pass4 ! GetCount
			expectMsg(limit)
		}
		
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
	}
	
	
	
}