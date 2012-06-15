package multilink.util

import akka.actor.{Actor, ActorLogging}
import akka.actor.Actor._
import Composable._
import Dispatcher._


trait ComposableActor extends Actor with Composable {
	this: Actor =>
	
	type Result = (Int, Dispatcher.DispatcherNode, Dispatcher.Direction, Any) => DispatcherMessages
	
	private val defaultFunction: PartialFunction[Any,Result] = {
		case _ => done
	}	
	
	protected def reply(y: Any)(generation: Int, node: Dispatcher.DispatcherNode, direction: Dispatcher.Direction, msg: Any) = Reply(generation, node, direction, List(y)) 
	protected def done(generation: Int, node: Dispatcher.DispatcherNode, direction: Dispatcher.Direction, msg: Any) = Done(generation, node, direction, msg) 
	
	def process: PartialFunction[Any, Result] 
	
	protected override def receive: Receive = {
		case Process(generation, thisNode, direction, msg) /* if process.isDefinedAt(msg) */=> 
			
			(process orElse defaultFunction)(msg)(generation, thisNode, direction, msg) match {
				case x: Done if thisNode.next != None => {
					val nextActorNode = thisNode.next.get
					nextActorNode.actorRef.forward(Process(generation, nextActorNode, direction, msg))
				}
				case x => sender ! x
			}
	}
}

trait LoggableComposableActor extends ComposableActor with ActorLogging{
	import com.typesafe.config.ConfigFactory
	
	val debugMsg = ConfigFactory.load().getBoolean("akka.actor.debug.receive")
	
	override abstract def receive : Receive = {
		case msg @ Process(_, _, direction, realMsg) => {
			if(debugMsg){
				val isHandled = process.isDefinedAt(realMsg) 
				log.debug("Received "+ (if(isHandled) "" else "un") +"handled "+direction+" message "+ realMsg)
			}
			super.receive(msg)
		}
	}
		
}