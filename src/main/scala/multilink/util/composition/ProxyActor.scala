package multilink.util.composition

import akka.actor.ActorRef
import akka.actor.Actor
import Composable._
import CompositionNetwork._
import akka.actor.FSM
//import multilink.util.composition.CompositionNetwork

object ProxyActor{
	sealed trait Messages
	case class Process(generation: Int, direction: Direction, msg: Any) extends Messages
	
	sealed trait State
	object Idle extends State
	object Working extends State
	
	sealed trait Data
	object NullData extends Data 
}

class ProxyActor[A <: Actor with Composable](val proxyTo: ActorRef, network: CompositionNetwork[A]) extends Actor with FSM[ProxyActor.State, ProxyActor.Data]{
	import ProxyActor._ 
	import CompositionNetwork._
	
	startWith(Idle, NullData)
	
	when(Idle){
		case Event(ProxyActor.Process(generation, direction, msg),_) => {
			network.incomingNodes.foreach(node => node.actorRef ! CompositionNetwork.Process(generation, node, Incoming, msg))
			goto(Working)
		}
	}
	
	
	
}