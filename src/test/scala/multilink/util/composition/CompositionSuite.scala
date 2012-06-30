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

object CompositionSuite{
	
	sealed trait Messages
	case object PassThrough extends Messages
	case object Reply extends Messages
	case class ReplyFrom(id: Int) extends Messages
	case object GetCount extends Messages
	case object Discharge extends Messages
	
	
	case class Replier(val id: Int) extends MultilinkActor with ComposableActor{
		def react = {
			case Reply => sender ! ReplyFrom(id)
		} 
	}

	case class Pass(val id: Int) extends MultilinkActor with ComposableActor{
		def react = {
			case PassThrough => //Nothing to do 
		} 
	}
	
	case class Counter(val id: Int) extends MultilinkActor with ComposableActor{
		val counter = Iterator from 0
		var soFar = 0
		
		def react = {
			case GetCount => sender ! soFar
			case msg => soFar = counter.next()
		}
	}
	
	case class Accumulator(val id: Int) extends MultilinkActor with ComposableActor{
		val accumulator: Queue[Messages] = Queue()
		
		def react = {
			case Discharge => accumulator.foreach(msg => sender ! msg)
			case msg : Messages => accumulator.enqueue(msg)
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
	
	"A composition" should {
		"allow you to message the lifted actors" in {
			val network = Replier(1).lift
			val networkActor = system.actorOf(Props(network))
			
			within(5 hours){
				networkActor ! Reply
				expectMsg(ReplyFrom(1))
				network ! Reply
				expectMsg(ReplyFrom(1))
			}
		}
		
		"allow you to reach thdsdse lifted actors" ignore {
			val network = Pass(1) >>> Pass(2) >>> Pass(3) >>> (Replier(4) &&& Replier(5) &&& Replier(6))
			val networkActor = system.actorOf(Props(network),"network")
			
			networkActor ! Reply
			expectMsgAllOf(ReplyFrom(4),ReplyFrom(5),ReplyFrom(6))
		}
	}
	
}