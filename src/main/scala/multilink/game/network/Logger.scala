package multilink.game.network

import akka.actor.{Actor, FSM, LoggingFSM}
import scala.collection.mutable.LinkedList
import multilink.util.composition.{Composable, ComposableActor}
import multilink.util.replication._
import multilink.util.MultilinkActor

object Logger {
	
	type State = Int
}

class Logger extends MultilinkActor with ComposableActor with ReplicatableActor[Logger.State]{
	
	val ids = Iterator from 0
	var log = new LinkedList[(Int, String)]()
	
	def getState = 0
	def setState(x: Logger.State) : Unit = 0
	
	def react() = {
		case anyMsg => 
			log +:= ((ids.next(), anyMsg.toString()))
			println(log)
	}

}

