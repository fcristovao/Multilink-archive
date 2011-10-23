package multilink.util

import akka.actor.{Actor, FSM}

trait ComposableFSM[S,D] extends FSM[S,D] with Composable {this: Actor =>
	import Composable._
	import Dispatcher._
	
	private final val ender: StateFunction = {
		case _ => stay
	}
	
	private def interceptor(stateFunction: StateFunction): StateFunction = {
		case Event(Process(generation, thisNode, direction, msg), stateData) => {
			val newState = (stateFunction orElse ender)(Event(msg, stateData))
			newState.replies match {
				case Nil => newState.copy(replies = List(Done(generation, thisNode, direction, msg)))
				case anythingElse => newState.copy(replies = List(Reply(generation, thisNode, direction, newState.replies)))
			}
		}
	}
	
	protected final def whenIn(stateName: S, stateTimeout: Timeout = None)(stateFunction: StateFunction) = {
		when(stateName, stateTimeout){
			new StateFunction{
				val concreteInterceptor = interceptor(stateFunction)
				
				def isDefinedAt(ev: Event) = concreteInterceptor.isDefinedAt(ev) || stateFunction.isDefinedAt(ev)
				def apply(ev: Event) = (concreteInterceptor orElse stateFunction)(ev)
			}
		}
	}
	
}
/*



*/