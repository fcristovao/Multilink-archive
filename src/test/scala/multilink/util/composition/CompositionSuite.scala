package multilink.util.composition

import akka.testkit.TestKit
import akka.actor.ActorSystem
import akka.testkit.ImplicitSender
import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfterAll
import multilink.util.MultilinkActor
import akka.actor.ActorRef
import akka.actor.Props

object CompositionSuite{
	
	sealed trait Messages
	case object PassThrough extends Messages
	case object Reply extends Messages
	case class ReplyFrom(id: Int) extends Messages
	
	
	case class Replier(val id: Int) extends MultilinkActor with ComposableActor{
		val iter = Iterator from 1
		
		def react = {
			case PassThrough => //Nothing to do 
			case Reply => sender ! ReplyFrom(id)
		} 
	}

	case class Pass(val id: Int) extends MultilinkActor with ComposableActor{
		val iter = Iterator from 1
		
		def react = {
			case PassThrough => //Nothing to do 
		} 
	}
}


class CompositionSuite extends TestKit(ActorSystem("TestSystem")) with ImplicitSender with FunSuite with BeforeAndAfterAll{
	import CompositionSuite._
	
	override def afterAll{
		system.shutdown()
	}
	
	test("just Testing"){
		val network = Pass(1) >>> Pass(2) >>> Pass(3) >>> (Replier(4) &&& Replier(5) &&& Replier(6))
		val networkActor = system.actorOf(Props(network),"network")
		
		networkActor ! PassThrough
		expectMsgAllOf(ReplyFrom(4),ReplyFrom(5),ReplyFrom(6))
	}
	
}