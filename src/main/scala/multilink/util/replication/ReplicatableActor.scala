package multilink.util.replication

import multilink.util.MultilinkActor

trait ReplicatableActor[State] extends MultilinkActor with Replicatable[State] {
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