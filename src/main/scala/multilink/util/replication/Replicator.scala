package multilink.util.replication

import akka.actor.Actor
import akka.actor.Actor._
import akka.actor.ActorRef
import multilink.util.MultilinkActor
import akka.actor.Props
import akka.event.LoggingReceive

object Replicator {
	
	sealed trait Messages
	case class Begin(master: Option[ActorRef] = None) extends Messages {
		def this(actorRef: ActorRef) = this(Some(actorRef))
	}
	
}

trait Replicator[A <: Actor with Replicator[A]] extends MultilinkActor {
	import Replicator._

	//private def getProps[A <: Actor with Replicator[A] : Manifest]() = Props[A]
	
	var msgTimestamp: Long = _
	var replicatingTo: List[ActorRef] = List()
	
	var isMaster: Boolean = false
	
	def uninitialized: Receive = LoggingReceive({
		case Begin(master) => {
			master match {
				case None => { // We are the masters
					isMaster = true
					replicatingTo = context.actorOf(context.props,"replicatable1") +: replicatingTo
					replicatingTo = context.actorOf(context.props,"replicatable2") +: replicatingTo
					
					replicatingTo.foreach(_ ! new Begin(self))
				}
				case Some(masterActorRef) => {
					// do nothing for now
				}
			}
			context.become(running)
		}
		case msg  => {
			self ! Begin(None); self ! msg
		} 
	})
	
	def running : Receive = LoggingReceive({
		case msg => {
			msgTimestamp = System.currentTimeMillis();
			replicate(msgTimestamp, msg)
			super.receive(msg)
		}
	})
	
	abstract override def receive: Receive = uninitialized
	
	def replicate(msgTimestamp: Long, msg: Any) = {
		replicatingTo.map(_ ! Replicatable.Replicate(self, msgTimestamp, msg))
	}
	
}