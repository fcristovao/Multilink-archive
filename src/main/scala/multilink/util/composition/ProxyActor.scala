package multilink.util.composition

import akka.actor.ActorRef
import akka.actor.Actor
import Composable._
import CompositionNetwork._
import akka.actor.FSM
import scala.collection.immutable.Map
import scala.collection.immutable.Queue
import akka.actor.LoggingFSM

object ProxyActor{
	sealed trait Messages
	case class Process(generation: Int, direction: Direction, msg: Any) extends Messages
	//case class Done(generation: Int, who: CompositionNode, direction: Direction, msg: Any) extends Messages(generation, who, direction)
	
	sealed trait State
	case object Idle extends State
	case object Processing extends State
	
	case class Data(
			messagesYetProcessing: Map[(Option[CompositionNode], Any, Direction), Int] = Map.empty,
			answersReceived: Map[ActorRef,Queue[Any]] = Map.empty,
			replies: Queue[Any] = Queue(),
			currentGeneration: Int = -1,
			awaitingProcessMsgs: Queue[CompositionNetwork.Process] = Queue())
}

class ProxyActor[A <: Actor with Composable](val proxyTo: ActorRef) extends Actor with FSM[ProxyActor.State, ProxyActor.Data] with LoggingFSM[ProxyActor.State, ProxyActor.Data]{
	import ProxyActor._ 
	import CompositionNetwork._
	
	private def answerToOriginalSender(replies: Queue[Any]) = {
		for(msg <- replies){
			proxyTo ! msg
		}
	}
	
	
	startWith(Idle, Data())
	
	when(Idle){
		case Event(CompositionNetwork.Process(generation, node, direction, msg), Data(_, _, _, _, awaitingProcessMsgs)) if (node.parent == None && node.previous == None)=> {
			node.actorRef ! CompositionNetwork.Process(generation, node, Incoming, msg)
			goto(Processing) using Data(currentGeneration = generation, messagesYetProcessing = Map((node.parent, msg, direction) -> 1), awaitingProcessMsgs = awaitingProcessMsgs)
		}
	}
		
	when(Processing){
		case Event(CompositionNetwork.Process(generation, node, direction, msg), data @ Data(messagesYetProcessing, _, _, currentGeneration, _)) if(generation == currentGeneration) => {
			val newNrOfMsgs = messagesYetProcessing.getOrElse((node.parent, msg, direction),0) + 1
			node.actorRef ! CompositionNetwork.Process(generation, node, Incoming, msg)
			stay using data.copy(messagesYetProcessing + ((node.parent, msg, direction) -> newNrOfMsgs))
		}
		
		case Event(processMsg @ CompositionNetwork.Process(generation, node, direction, msg), data @ Data(_,_,_, currentGeneration, awaitingProcessMsgs)) if(generation > currentGeneration && node.parent == None && node.previous == None) => {
			stay using data.copy(awaitingProcessMsgs = awaitingProcessMsgs.enqueue(processMsg))
		}
		
		case Event(doneMsg @ CompositionNetwork.Done(generation, node, direction, msg), data @ Data(messagesYetProcessing, answersReceived, replies, currentGeneration, _)) if(answersReceived.contains(sender) && generation == currentGeneration) => {
			def helper(node: Option[CompositionNode], msg: Any, map: Map[(Option[CompositionNode], Any, Direction), Int] = Map()) : Map[(Option[CompositionNode], Any, Direction), Int] = {
				if(node.isEmpty)
					map + ((node, msg, Outgoing) -> 1)
				else
					helper(node.get.parent, msg, map + ((node, msg, Outgoing) -> 1))
			}
			
			val queue = answersReceived(sender)

			self ! doneMsg
			
			val remainingNrOfMessages = node.next(direction) match {
				case None => messagesYetProcessing((node.parent, msg, direction)) 
				case Some(nextNode) => messagesYetProcessing((node.parent, msg, direction)) - 1
			}
			
			val newMsgsYetProcessing =
				(for(msg <- queue) yield {
					self ! Done(generation, node, Outgoing, msg)
					
					helper(node.parent, msg)
				}).reduce(_ ++ _)
			
			stay using data.copy(messagesYetProcessing.updated((node.parent, msg, direction), remainingNrOfMessages) ++ newMsgsYetProcessing, answersReceived - sender, replies ++ queue)
		}
		
		case Event(CompositionNetwork.Done(generation, node, direction, msg), data @ Data(messagesYetProcessing, answersReceived, replies, currentGeneration, awaitingProcessMsgs)) if(!answersReceived.contains(sender) && generation == currentGeneration) => {
			//Some actor has finalized its processing of the message
			
			val remainingNrOfMessages = node.next(direction) match {
				case None => messagesYetProcessing((node.parent, msg, direction)) - 1 
				case Some(nextNode) => messagesYetProcessing((node.parent, msg, direction)) 
			}
			
			if(remainingNrOfMessages == 0) {
				val newMessagesYetProcessing = messagesYetProcessing - ((node.parent,msg, direction))
				
				if(node.parent.isEmpty){
					if(newMessagesYetProcessing.isEmpty){
						//There are no other messages to wait, so just finish everything up
						answerToOriginalSender(replies)
						
						if(awaitingProcessMsgs.isEmpty){
							goto(Idle) using Data()
						} else{
							val (msg, newQueue) = awaitingProcessMsgs.dequeue
							self ! msg
							goto(Idle) using Data(awaitingProcessMsgs = newQueue)
						}
					} else {
						//nothing to do
						stay using data.copy(messagesYetProcessing = newMessagesYetProcessing)
					}
				} else {
					self ! Done(generation, node.parent.get, direction, msg)
					stay using data.copy(messagesYetProcessing = newMessagesYetProcessing)
				}
			} else {
				node.next(direction) match {
					case Some(nextNode) => nextNode.actorRef ! CompositionNetwork.Process(generation, nextNode, direction, msg)
					case None => //Nothing to do
				}
				stay using data.copy(messagesYetProcessing.updated((node.parent, msg, direction), remainingNrOfMessages))
			}
			
		}
		case Event(someMsg, data @ Data(_, answersReceived, _, _, _))  => {
			val queueSoFar = answersReceived.getOrElse(sender,Queue())
			
			stay using data.copy(answersReceived = answersReceived + (sender -> (queueSoFar enqueue someMsg)))
		}
	}
	
	/* Backup
	node.next(direction) match {
				case None => { //reached the end of the path
					val remainingNrOfMessages = messagesYetProcessing((node.parent, msg, direction)) - 1
					if(remainingNrOfMessages == 0) {
						val newMessagesYetProcessing = messagesYetProcessing - ((node.parent,msg, direction))
						
						if(node.parent.isEmpty){
							if(newMessagesYetProcessing.isEmpty){
								//There are no other messages to wait
								answerToOriginalSender(replies)
								stay using PAData()
							} else {
								//nothing to do
								stay using data.copy(messagesYetProcessing = newMessagesYetProcessing)
							}
						} else {
							self ! Done(generation, node.parent.get, direction, msg)
							stay using data.copy(messagesYetProcessing = newMessagesYetProcessing)
						}
					} else {
						stay using data.copy(messagesYetProcessing.updated((node.parent, msg, direction), remainingNrOfMessages))
					}
				}
				case Some(nextNode) => {
					nextNode.actorRef ! CompositionNetwork.Process(generation, nextNode, direction, msg)
					stay
				}
			}
			*/
	
}