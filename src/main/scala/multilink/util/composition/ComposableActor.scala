package multilink.util.composition

import akka.actor.{Actor, ActorLogging}
import Composable._
import CompositionNetwork._
import multilink.util.MultilinkActor

trait ComposableActor extends MultilinkActor with Composable{
	/*
	type Result = (Int, CompositionNode, Direction, Any) => CompositionNetwork.Messages
	
	private val defaultFunction: PartialFunction[Any,Result] = {
		case _ => done
	}	
	
	protected def reply(y: Any)(generation: Int, node: CompositionNode, direction: Direction, msg: Any) = Reply(generation, node, direction, List(y)) 
	protected def done(generation: Int, node: CompositionNode, direction: Direction, msg: Any) = Done(generation, node, direction, msg) 
	
	def process: PartialFunction[Any, Result] 
	
	//override def sender = context.actorOf(Props(new Actor{def receive() = {case _ => }}))
	*/
	abstract override def receive: Receive = {
		case Process(generation, thisNode, direction, msg) => {
			if(super.receive.isDefinedAt(msg)){
				super.receive(msg)
			}
			sender ! Done(generation, thisNode, direction, msg)
		}
		case msg if super.receive.isDefinedAt(msg) => super.receive(msg)
	}
	
}

/*
trait LoggableComposableActor extends ComposableActor with ActorLogging{
	override abstract def receive : Receive = {
		case msg @ Process(_, _, direction, realMsg) => {
			val isHandled = process.isDefinedAt(realMsg) 
			log.debug("Received "+ (if(isHandled) "" else "un") +"handled "+direction+" message "+ realMsg)
			super.receive(msg)
		}
	}
		
}
*/