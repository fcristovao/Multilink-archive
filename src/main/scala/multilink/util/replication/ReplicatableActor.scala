package multilink.util.replication

import multilink.util.MultilinkActor
import akka.event.LoggingReceive

trait ReplicatableActor[State] extends MultilinkActor with Replicatable[State] {
	import multilink.util.replication.Replicator
	
	def getState: State
	def setState(state: State) : Unit
	
	abstract override def receive: Receive = LoggingReceive({
		case Replicatable.Replicate(master, timestamp, msg) if super.receive.isDefinedAt(msg) => {
			setTimestampFor(msg, timestamp)
			super.receive(msg)
		}
		case msg if super.receive.isDefinedAt(msg) => super.receive(msg)
	})

}