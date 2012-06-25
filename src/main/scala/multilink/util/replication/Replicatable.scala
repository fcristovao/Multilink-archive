package multilink.util.replication

import akka.actor.ActorRef

trait Replicatable[State]{
	private var timestamp: Long = _
	
	def getTimestampFor(msg: Any): Long = timestamp
	private[replication] def setTimestampFor(msg: Any, timestamp: Long) = this.timestamp = timestamp
}

object Replicatable {
	sealed trait Messages
	case class Replicate(master: ActorRef, timestamp: Long, msg: Any)
}