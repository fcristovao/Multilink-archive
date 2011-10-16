package multilink.util

import akka.actor.{Actor, FSM}

trait ComposableFSM[S,D] extends FSM[S,D] with Composable {this: Actor =>
	import Composable._
	
	val ender: StateFunction = {
		case _ => stay
	}
	
	protected final def whenIn(stateName: S, stateTimeout: Timeout = None)(stateFunction: StateFunction) = {
		when(stateName, stateTimeout){
			case anything => {
				val newState = (stateFunction orElse ender)(anything)
				newState.replies match {
					case Nil => newState.copy(replies = List(Done))
					case anythingElse => newState.copy(replies = anythingElse.map(Reply(_)))
				}
			}
		}
	}
	
}
