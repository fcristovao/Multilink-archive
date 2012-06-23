package multilink.util.composition

import akka.actor.{Actor, ActorLogging}
import akka.actor.Actor._
import Composable._
import CompositionNetwork._

trait ComposableActor extends Actor with Composable {
	this: Actor =>
	
	type Result = (Int, CompositionNode, Direction, Any) => CompositionNetwork.Messages
	
	private val defaultFunction: PartialFunction[Any,Result] = {
		case _ => done
	}	
	
	protected def reply(y: Any)(generation: Int, node: CompositionNode, direction: Direction, msg: Any) = Reply(generation, node, direction, List(y)) 
	protected def done(generation: Int, node: CompositionNode, direction: Direction, msg: Any) = Done(generation, node, direction, msg) 
	
	def process: PartialFunction[Any, Result] 
	
	protected override def receive: Receive = {
		case Process(generation, thisNode, direction, msg) /* if process.isDefinedAt(msg) */=> 
			
			(process orElse defaultFunction)(msg)(generation, thisNode, direction, msg) match {
				case x: Done if thisNode.next(direction) != None => {
					val nextActorNode = thisNode.next(direction).get
					nextActorNode.actorRef.forward(Process(generation, nextActorNode, direction, msg))
				}
				case x => sender ! x
			}
	}
}

trait LoggableComposableActor extends ComposableActor with ActorLogging{
	override abstract def receive : Receive = {
		case msg @ Process(_, _, direction, realMsg) => {
			val isHandled = process.isDefinedAt(realMsg) 
			log.debug("Received "+ (if(isHandled) "" else "un") +"handled "+direction+" message "+ realMsg)
			super.receive(msg)
		}
	}
		
}