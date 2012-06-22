package multilink.util.replication

import akka.actor.Actor
import akka.actor.Actor._
import akka.actor.ActorRef

trait Replicator extends Actor {
	this: Actor =>

	var msgTimestamp: Long = _
	
	def isMaster: Boolean = true
		
	abstract override def receive: Receive = {
		case msg => {
			msgTimestamp = System.currentTimeMillis();
			replicate(msgTimestamp, msg)
			super.receive(msg)
		}
	}
	
	def replicate(msgTimestamp: Long, msg: Any) = {
		println("Replicating msg: "+msg+" with TS: "+msgTimestamp)
	}
	
}