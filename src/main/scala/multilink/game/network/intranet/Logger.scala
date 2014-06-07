package multilink.game.network.intranet

import akka.actor.{Actor, FSM, LoggingFSM}
import scala.collection.mutable.LinkedList
import multilink.util.composition.{Composable, ComposableActor}

object Logger {
	
	type State = Int
}

trait Logger extends ComposableActor {
	
	val ids = Iterator from 0
	var log = new LinkedList[(Int, Any)]()
	
	def getState = 0
	def setState(x: Logger.State) : Unit = 0
	
	abstract override def receive = {
		case anyMsg => 
			log +:= ((ids.next(), anyMsg))
			println(log)
	}

}

