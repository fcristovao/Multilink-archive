package multilink.util.composition

import Composable._
import akka.actor.{Actor, ActorRef, ActorRefFactory, Props}
import scala.collection.mutable




object CompositionNetwork{

	sealed abstract class Messages(generation: Int, who: CompositionNode, direction: Direction) 
	case class Process(generation: Int, who: CompositionNode, direction: Direction, msg: Any) extends Messages(generation, who, direction)
	case class Done(generation: Int, who: CompositionNode, direction: Direction, msg: Any) extends Messages(generation, who, direction)
	case class Reply(generation: Int, who: CompositionNode, direction: Direction, replies: List[Any]) extends Messages(generation, who, direction)
	
	sealed trait Direction 
	case object Incoming extends Direction
	case object Outgoing extends Direction
	
	private[util] abstract class CompositionNode(
			val nrOfPaths: Int,
			val actorName: String,
			val actorRef: ActorRef,
			val receiveIncoming: Boolean,
			val receiveOutgoing: Boolean,
			val pathNr: Int = 1, 
			val previous: Option[CompositionNode] = None, 
			var next: Option[CompositionNode] = None,
			var parent: Option[CompositionNode] = None) {
		
		import scala.collection.immutable
	
		def startNodes: List[CompositionNode]
		
		def endNodes: List[CompositionNode]
		
		def receives(direction: Direction) = direction match {case Incoming => receiveIncoming; case Outgoing => receiveOutgoing}
		
		private def _next(direction: Direction): Option[CompositionNode] = {
			var result = direction match {case Incoming => next; case Outgoing => previous }
			var found = false
			while(result != None && !found){
				val isAdmissible = result.get.receives(direction)
				if(isAdmissible)
					found = true
				else
					result = direction match {case Incoming => result.get.next; case Outgoing => result.get.previous }
			}
			result
		}
		
		private lazy val nextMap = immutable.Map[Direction,Option[CompositionNode]](Incoming -> _next(Incoming), Outgoing-> _next(Outgoing)) 
		
		//memoized version of _next
		def next(direction: Direction): Option[CompositionNode] = nextMap(direction)
	}
	
	private[util] class LiftNode(
			actorName: String,
			actorRef: ActorRef,
			receiveIncoming: Boolean,
			receiveOutgoing: Boolean,
			pathNr: Int = 1, 
			previous: Option[CompositionNode] = None, 
			next: Option[CompositionNode] = None,
			parent: Option[CompositionNode] = None) extends CompositionNode(1, actorName, actorRef, receiveIncoming, receiveOutgoing, pathNr, previous, next, parent){
		
		def startNodes: List[CompositionNode] = List(this)
		
		def endNodes: List[CompositionNode] = List(this)
	}
	
	private[util] class CompNode(
			val startNodes: List[CompositionNode],
			val endNodes: List[CompositionNode],
			actorName: String,
			actorRef: ActorRef,
			receiveIncoming: Boolean,
			receiveOutgoing: Boolean,
			pathNr: Int = 1, 
			previous: Option[CompositionNode] = None, 
			next: Option[CompositionNode] = None,
			parent: Option[CompositionNode] = None) extends CompositionNode(1, actorName, actorRef, receiveIncoming, receiveOutgoing, pathNr, previous, next, parent){
		

	}
	
	private[util] class SplitterNode(
			nrOfPaths: Int,
			val startNodes: List[CompositionNode],
			val endNodes: List[CompositionNode],
			actorName: String,
			actorRef: ActorRef,
			receiveIncoming: Boolean,
			receiveOutgoing: Boolean,
			pathNr: Int = 1, 
			previous: Option[CompositionNode] = None, 
			next: Option[CompositionNode] = None,
			parent: Option[CompositionNode] = None) extends CompositionNode(nrOfPaths, actorName, actorRef, receiveIncoming, receiveOutgoing, pathNr, previous, next, parent){
		
		assert(!(startNodes == Nil && endNodes == Nil))
		
		
		
		private def filterNodes(list: List[CompositionNode], direction: Direction): List[CompositionNode] = {
			list.map(node => {
				if(node.receives(direction))
					Some(node)
				else
					node.next(direction)	
			}).collect({case Some(x) => x})
		}
		
		val incomingNodes = filterNodes(startNodes, Incoming)
		val outgoingNodes = filterNodes(endNodes, Outgoing)
		
	}
	
	private[util] class SplitterNodeDispatcher() extends Actor {
		def receive() = {
			case CompositionNetwork.Process(generation, node: SplitterNode, direction, msg) =>
				direction match {
					case Incoming => node.incomingNodes.foreach(node => node.actorRef ! CompositionNetwork.Process(generation, node, direction, msg))
					case Outgoing => node.outgoingNodes.foreach(node => node.actorRef ! CompositionNetwork.Process(generation, node, direction, msg))
				}
		}
	}
	
	private[util] class CompNodeDispatcher() extends Actor {
		def receive() = {
			case CompositionNetwork.Process(generation, node: CompNode, direction, msg) =>
				direction match {
					case x =>
					//case Incoming => node.incomingNodes.foreach(node => node.actorRef ! CompositionNetwork.Process(generation, node, direction, msg))
					//case Outgoing => node.outgoingNodes.foreach(node => node.actorRef ! CompositionNetwork.Process(generation, node, direction, msg))
				}
		}
	}
	
	/*
	 * This version creates only ONE actor instance in the case of the *same* lift is found in the composition.
	 * E.g.:
	 * val tmp = Lift(new Firewall())
	 * tmp >>> new Gateway >>> tmp
	 * ^-- This would create only one Firewall instance (although it would receive the same message twice)
	 */
	def fromArrowOperator[A <: Actor with Composable](comb: ArrowOperator[A], actorRefFactory: ActorRefFactory): CompositionNode = {
		val alreadyCreated = scala.collection.mutable.Map[() => Actor with Composable, ActorRef]()
		var nameCounter = 1

		def helper(comb: ArrowOperator[A], pathNr: Int = 1, previous: Option[CompositionNode] = None): (Option[CompositionNode], Option[CompositionNode]) = {
			comb match {
				case lifted @ Lift(actorFactory, inbound, outbound) => {
					val actorRef = 
						alreadyCreated.get(actorFactory) match {
							case None => {
								val tmp = actorRefFactory.actorOf(Props(actorFactory), nameCounter + "-" + lifted.actorName)
								nameCounter+=1
								alreadyCreated += (actorFactory -> tmp)
								tmp
							}
							case Some(x) => {
								x
							}
						}
					
					val result = Some(new LiftNode(lifted.actorName, actorRef, inbound, outbound, pathNr, previous))
					(result,result)
				}
				case Composition(composables) => {
					def anotherHelper(listOfComposables: List[ArrowOperator[A]], previous: Option[CompositionNode]): (Option[CompositionNode], Option[CompositionNode]) = {
						listOfComposables match {
							case head :: tail => {
								val (result, newerPrevious) = helper(head, pathNr, previous)
								val (next, newestPrevious) = anotherHelper(tail, newerPrevious)
								result match {
									case None => {
										(next, newestPrevious)
									}
									case _ => {
										result.get.next = next
										(result, newestPrevious)
									}
								}
							}
							case Nil => (None, previous)
						}
					}
					anotherHelper(composables, previous)
				}
				case sp @ Splitter(splitted) => {
					val intermediate = (splitted zip (1 until splitted.size+1)).map({case (comb, pathNr) => helper(comb, pathNr)})
					val nrOfPaths = intermediate.size
					val (startNodes, endNodes) = intermediate.foldLeft((List[CompositionNode](),List[CompositionNode]()))((tupleList, tupleOption) => (tupleOption._1.toList ::: tupleList._1, tupleOption._2.toList ::: tupleList._2))
		
					val actorRef = actorRefFactory.actorOf(Props[SplitterNodeDispatcher], nameCounter + "-Splitted")
					nameCounter+=1
					
					val result = Some(new SplitterNode(nrOfPaths, startNodes, endNodes, "Splitted", actorRef, sp.incoming, sp.outgoing, pathNr, previous))
					
					for(node <- startNodes){
						var current : Option[CompositionNode] = Some(node)
						while(current != None){
							current.get.parent = result
							current = current.get.next
						}
					}
					
					(result,result)
				}
			}
		}
			
		comb match {
			case Splitter(splitted) => {
				helper(comb)._1.get
			}
			case anyOther => {
				val (startNodes, endNodes) = helper(comb)
		
				val actorRef = actorRefFactory.actorOf(Props[SplitterNodeDispatcher], nameCounter + "-Splitted")
				nameCounter+=1
					
				val result = Some(new SplitterNode(1, List(startNodes.get), List(endNodes.get), "Splitted", actorRef, anyOther.incoming, anyOther.outgoing))
				
				for(node <- startNodes){
					var current : Option[CompositionNode] = Some(node)
					while(current != None){
						current.get.parent = result
						current = current.get.next
					}
				}
					
				result.get
			}
		}	
	}
	
	
	
	
	
}

/*
class CompositionNetwork[A <: Actor with Composable](comb: ArrowOperator[A]) extends Actor {
	import CompositionNetwork._
	
	val compositionNode = CompositionNetwork.fromArrowOperator(comb, context)
	//val incomingNodes = filterNodes(startNodes, Incoming)
	//val outgoingNodes = filterNodes(endNodes, Outgoing)
	
	
	
	private sealed abstract class State(val sender: ActorRef, var pathsCompleted: Int = 0, var replies: List[Any] = List())
	private case class CommonState(override val sender: ActorRef) extends State(sender)
	private case class ExtendedState(override val sender: ActorRef, val processMsgReceived: Process) extends State(sender)
	
	/*
	 * expectedNrMsgs: Expected Number of messages to reach the beginning of the path (with the Outgoing direction, that is, from the end to the beginning) 
	 * replies: accumulated reply messages along this path. When the beginning of path is reached, these are added to the global replies
	 * nrMsgsSoFar: number of messages that reached the beginning of the path so far
	 */
	private case class ReplyingState(var expectedNrMsgs: Int, var replies: List[Any], var nrMsgsSoFar: Int = 0)
	
	private val generation = Iterator from 0
	private val pendentSenders = mutable.Map[Int, State]()
	private val awaitingPathReplies = mutable.Map[(Int, Int), ReplyingState]()
	
	private def tryToAnswer(generation: Int, state: State): Boolean = {
		if(state.pathsCompleted == nrOfPaths){
			state match {
				case ExtendedState(sender, Process(receivedGeneration, node, direction, msg)) => {
					state.replies match {
					  case Nil => sender ! Done(receivedGeneration, node, direction, msg)
					  case listOfReplies => sender ! Reply(receivedGeneration, node, direction, listOfReplies)
					}
				}
				case state: CommonState => {
					state.replies foreach (state.sender ! _)
				}
			}
			pendentSenders -= generation
			true
		} else {
			false
		}
	}
	
	protected override def receive = {
			case Done(generation, fromNode, direction, msg) => {
				
				fromNode.next(direction) match {
					case Some(node) => {
						node.actorRef ! Process(generation, node, direction, msg)
					}
					case None => {
						val state = pendentSenders(generation)
						direction match {
							case Incoming => {
								//We've reached the end of a path with a Done message, so no outgoing path has to be followed (because there were no replies along this path)
								state.pathsCompleted += 1
								tryToAnswer(generation, state)
							}
							case Outgoing => {
								val replyingState = awaitingPathReplies((generation, fromNode.pathNr))
								replyingState.nrMsgsSoFar += 1
								if(replyingState.nrMsgsSoFar == replyingState.expectedNrMsgs){
									state.pathsCompleted += 1
									state.replies = state.replies ++ replyingState.replies 
									awaitingPathReplies -= ((generation, fromNode.pathNr))
									tryToAnswer(generation, state)
								}
							}
						}
					}
				}
			}
			
			case Reply(generation, fromNode, direction, replies) => {
				val state = pendentSenders(generation)
				direction match {
					case Incoming => {
						fromNode.previous match {
							case None => { // There's no node that would process an outgoing message 
								state.pathsCompleted += 1
								state.replies = state.replies ++ replies
								tryToAnswer(generation, state)
							}
							case Some(node) => {
								awaitingPathReplies += (generation, node.pathNr) -> ReplyingState(replies.size, replies)
								replies.foreach(node.actorRef ! Process(generation, node, Outgoing, _)) 
							}
						}
					}
					case Outgoing => {
						val replyingState = awaitingPathReplies((generation, fromNode.pathNr))
						fromNode.previous match {
							case None => { // There's no more nodes outgoing in this path
								replyingState.nrMsgsSoFar += 1
								if(replyingState.nrMsgsSoFar == replyingState.expectedNrMsgs){
									state.pathsCompleted += 1
									state.replies = state.replies ++ replyingState.replies ++ replies
									awaitingPathReplies -= ((generation, fromNode.pathNr))
									tryToAnswer(generation, state)
								}
							}
							case Some(node) => {
								replyingState.expectedNrMsgs += replies.size
								replyingState.replies = replyingState.replies ++ replies
								replyingState.replies.foreach(node.actorRef ! Process(generation, node, Outgoing, _)) 
							}
						}
					}
				}
			}
			
			case processMsg @ Process(generationFromFather, thisNode, direction, msg)  => {
				assert(thisNode.actorRef==self)
				
				val nextGen = generation.next()
				val state = ExtendedState(sender, processMsg)
				pendentSenders += nextGen -> state
						
				direction match {
					case Incoming => {
						if(!incomingNodes.isEmpty) {
							state.pathsCompleted = nrOfPaths - incomingNodes.size
							incomingNodes.foreach(node => node.actorRef ! Process(nextGen, node, direction, msg))
						} else {
							sender ! Done(generationFromFather, thisNode, direction, msg)
						}
					}
					case Outgoing => {
						if(!outgoingNodes.isEmpty) {
							state.pathsCompleted = nrOfPaths - outgoingNodes.size
							outgoingNodes.foreach(node => {awaitingPathReplies += (nextGen, node.pathNr) -> ReplyingState(1, Nil); node.actorRef ! Process(nextGen, node, direction, msg) })
						} else {
							sender ! Done(generationFromFather, thisNode, direction, msg)
						}
					}
				}
			}
		
		case anyOtherMsg => {
			if(!incomingNodes.isEmpty) {
				val nextGen = generation.next()
				pendentSenders += nextGen -> CommonState(sender)
				
				incomingNodes.foreach(node => node.actorRef ! Process(nextGen, node, Incoming, anyOtherMsg))
			}
		}
	}
}

*/