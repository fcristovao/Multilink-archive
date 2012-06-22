package multilink.util.composition

import akka.actor.{Actor, FSM}
import akka.util.Duration
import multilink.util.MultilinkFSM

trait ComposableFSM[S,D] extends MultilinkFSM[S,D] with Composable {
	this: Actor =>
		
	import Composable._
	
	abstract override def whenIn(stateName: S, stateTimeout: Duration = null)(stateFunction: StateFunction) = {
		val interceptor: StateFunction = {
			case Event(Process(generation, thisNode, direction, msg), stateData) => {
				val newState = (stateFunction orElse ender)(Event(msg, stateData))
				newState.replies match {
					case Nil => newState.copy(replies = List(Done(generation, thisNode, direction, msg)))
					case anythingElse => newState.copy(replies = List(Reply(generation, thisNode, direction, newState.replies)))
				}
			}
		}
		super.whenIn(stateName,stateTimeout)(interceptor orElse stateFunction)
	}
	
}
