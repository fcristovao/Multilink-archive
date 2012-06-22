package multilink.game.network

import akka.actor.{Actor, FSM, LoggingFSM}
import scala.collection.mutable.LinkedList

import multilink.util.composition.ComposableActor
import multilink.util.replication._

object Logger {
	
	type State = Int
}

class Logger extends Actor with ComposableActor with ReplicatableActor[Logger.State] {
	
	val ids = Iterator from 0
	var log = new LinkedList[(Int, String)]()
	
	def getState = 0
	def setState(x: Logger.State) = 0
	
	def process() = {
		case anyMsg => 
			log +:= ((ids.next(), anyMsg.toString()))
			println(log)
			done
	}

}