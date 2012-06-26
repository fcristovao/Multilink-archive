package multilink.util.composition

import akka.actor.ActorRef
import akka.actor.Actor
import Composable._
import CompositionNetwork._
import akka.actor.FSM
//import multilink.util.composition.CompositionNetwork

import scala.collection.mutable.Map

object ProxyActor{
	sealed trait Messages
	case class Process(generation: Int, direction: Direction, msg: Any) extends Messages
	//case class Done(generation: Int, who: CompositionNode, direction: Direction, msg: Any) extends Messages(generation, who, direction)
	
	sealed trait State
	object Idle extends State
	object ProcessingIncoming extends State
	
	sealed trait Data
	object NullData extends Data 
	case class PAData(map: Map[CompositionNode, Int]) extends Data
}

class ProxyActor[A <: Actor with Composable](val proxyTo: ActorRef, network: CompositionNode) extends Actor with FSM[ProxyActor.State, ProxyActor.Data]{
	import ProxyActor._ 
	import CompositionNetwork._
	
	startWith(Idle, NullData)
	
	when(Idle){
		case Event(CompositionNetwork.Process(generation, network, Incoming, msg),_) => {
			network.actorRef ! CompositionNetwork.Process(generation, network, Incoming, msg)
			goto(ProcessingIncoming) using PAData(Map.empty)
		}
	}
	
	when(ProcessingIncoming){
		case Event(CompositionNetwork.Done(generation, node, Incoming, msg), PAData(map)) => {
			//Some path has reached it's end
			node.parent match {
				case None => { //We're at the top level of the paths 
					
				}
				case Some(parentNode) =>
					val nrOfPathsCompleted = map.getOrElse(parentNode, 0) + 1
					if(parentNode.nrOfPaths == nrOfPathsCompleted){
						map -= parentNode
						self ! Done(generation, parentNode, Incoming, msg)
					} else {
						map(parentNode) = nrOfPathsCompleted
					}
			}
			stay
		}
	}
	
	
	
}