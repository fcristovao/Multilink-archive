package multilink.util.replication

import akka.actor.Actor
import akka.actor.ActorLogging

trait ReplicatableActor[State] extends Actor with Replicatable[State] {
	import multilink.util.replication.Replicator
	
	def getState: State
	def setState(state: State) : Unit
	
	abstract override def receive: Receive = {
		case Replicatable.Replicate(master, timestamp, msg) => {
			setTimestampFor(msg, timestamp)
			super.receive(msg)
		}
	}

}