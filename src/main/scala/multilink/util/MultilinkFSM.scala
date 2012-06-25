package multilink.util

import akka.actor.{Actor, FSM}
import akka.util.Duration

trait MultilinkFSM[S,D] extends FSM[S,D]{
	this: Actor =>
		
	protected final val ender: StateFunction = {
		case _ => stay
	}
		
	protected def whenIn(stateName: S, stateTimeout: Duration = null)(stateFunction: StateFunction) = {
		when(stateName, stateTimeout)(stateFunction)
	}
	
}