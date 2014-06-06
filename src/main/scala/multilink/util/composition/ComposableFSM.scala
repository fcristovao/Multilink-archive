package multilink.util.composition

import akka.actor.{Actor, FSM}
import scala.concurrent.duration._
import multilink.util.MultilinkFSM

trait ComposableFSM[S,D] extends MultilinkFSM[S,D] with Composable {
	this: Actor =>
		
	import CompositionNetwork._
	
	abstract override def whenIn(stateName: S, stateTimeout: Duration = null)(stateFunction: StateFunction) = {
		val interceptor: StateFunction = {
			case Event(Process(generation, thisNode, direction, msg), stateData) => {
				val newState = (stateFunction orElse ender)(Event(msg, stateData))
				val doneMsg = List(Done(generation, thisNode, direction, msg)) 
				newState.replies match {
					case Nil => newState.copy(replies = doneMsg)
					case anythingElse => newState.copy(replies = newState.replies ::: doneMsg)
				}
			}
		}
		super.whenIn(stateName,stateTimeout)(interceptor orElse stateFunction)
	}
	
}
