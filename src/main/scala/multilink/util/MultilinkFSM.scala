package multilink.util

import akka.actor.{Actor, FSM}
import scala.concurrent.duration._

trait MultilinkFSM[S,D] extends Actor with FSM[S,D]{
		
	protected final val ender: StateFunction = {
		case _ => stay
	}
		
	protected def whenIn(stateName: S, stateTimeout: FiniteDuration = null)(stateFunction: StateFunction) = {
		when(stateName, stateTimeout)(stateFunction)
	}
	
}