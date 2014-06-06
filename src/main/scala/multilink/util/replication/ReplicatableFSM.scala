package multilink.util.replication

import akka.actor.{Actor, FSM}
import scala.concurrent.duration._

import multilink.util.MultilinkFSM

trait ReplicatableFSM[S,D] extends MultilinkFSM[S,D] with Replicatable[D]{
	this: Actor =>

	abstract override def whenIn(stateName: S, stateTimeout: Duration = null)(stateFunction: StateFunction) = {
		val interceptor: StateFunction = {
			case Event(Replicatable.Replicate(master, timestamp, msg), stateData) => {
				setTimestampFor(msg,timestamp)
				(stateFunction orElse ender)(Event(msg, stateData))
			}
		}
		super.whenIn(stateName,stateTimeout)(interceptor orElse stateFunction)
	}
}