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
	
	private[util] abstract class CompositionNode1(
			val receiveIncoming: Boolean,
			val receiveOutgoing: Boolean,
			val parent: Option[CompositionNode] = None,
			val pathNr: Int = 1, 
			val previous: Option[CompositionNode] = None, 
			var next: Option[CompositionNode] = None) {
		
		import scala.collection.immutable
	
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
	
	private[util] case class LiftNode(
			val masterActor: ActorRef,
			val actorName: String,
			val actorRef: ActorRef,
			val receiveIncoming: Boolean,
			val receiveOutgoing: Boolean,
			val pathNr: Int = 1, 
			val previous: Option[CompositionNode] = None, 
			var next: Option[CompositionNode] = None) {
		
	}
	
	private[util] case class SplitterNode(
			val nrOfPaths: Int,
			val startNodes: List[CompositionNode],
			val endNodes: List[CompositionNode],
			receiveIncoming: Boolean,
			receiveOutgoing: Boolean,
			parent: Option[CompositionNode] = None,
			pathNr: Int = 1, 
			previous: Option[CompositionNode] = None, 
			next: Option[CompositionNode] = None) extends CompositionNode1(receiveIncoming, receiveOutgoing, parent, pathNr, previous, next){
		
	}
	
	
	private[util] case class CompositionNode(
			val masterActor: ActorRef,
			val actorName: String,
			val actorRef: ActorRef,
			val receiveIncoming: Boolean,
			val receiveOutgoing: Boolean,
			val pathNr: Int = 1, 
			val previous: Option[CompositionNode] = None, 
			var next: Option[CompositionNode] = None) {
		
		import scala.collection.immutable
	
		override def toString(): String = {
			"∆("+actorName+")" + (if(!next.isEmpty) ("⇄"+next.get.toString()) else "")
		}
		
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
	
	
	/*
	 * This version creates only ONE actor instance in the case of the *same* lift is found in the composition.
	 * E.g.:
	 * val tmp = Lift(new Firewall())
	 * tmp >>> new Gateway >>> tmp
	 * ^-- This would create only one Firewall instance (although it would receive the same message twice)
	 */
	def fromArrowOperator[A <: Actor with Composable](comb: ArrowOperator[A], masterActor: ActorRef, actorRefFactory: ActorRefFactory): (Int, List[CompositionNode], List[CompositionNode]) = {
		val alreadyCreated = scala.collection.mutable.Map[() => Actor with Composable, ActorRef]()
		var nameCounter = 1

		def helper(comb: ArrowOperator[A], pathNr: Int = 0, previous: Option[CompositionNode] = None): (Option[CompositionNode], Option[CompositionNode]) = {
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
					
					val result = Some(CompositionNode(masterActor, lifted.actorName, actorRef, inbound, outbound, pathNr, previous))
					/*(inbound, outbound) match {
						case (true, true) => (result, result)
						case (false, true) => (None, result)
						case (true, false) => (result, previous)
						//case (false, false) => Should never happen, so if it does, let it be signalled with the exception
					}*/
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
					val actorRef = actorRefFactory.actorOf(Props(new CompositionNetwork(sp)), nameCounter + "-Splitted")
					val result = Some(CompositionNode(masterActor, "Splitted", actorRef, sp.incoming, sp.outgoing, pathNr, previous))

					/*
					(sp.incoming, sp.outgoing) match {
						case (true, true) => (result, result)
						case (false, true) => (None, result)
						case (true, false) => (result, previous)
						//case (false, false) => Should never happen, so if it does, let it be signalled with the exception
					}
					*/
					(result,result)
				}
			}
		}
			
		val intermediate = comb match {
			case Splitter(splitted) => {
				(splitted zip (0 until splitted.size)).map({case (comb, pathNr) => helper(comb, pathNr)})
			}
			case anyOther => List(helper(comb))
		}	
		val nrOfPaths = intermediate.size
		val (startNodes, endNodes) = intermediate.foldLeft((List[CompositionNode](),List[CompositionNode]()))((tupleList, tupleOption) => (tupleOption._1.toList ::: tupleList._1, tupleOption._2.toList ::: tupleList._2)) 
	
		(nrOfPaths, startNodes, endNodes)
	}
	
	
	private def filterNodes(list: List[CompositionNode], direction: Direction): List[CompositionNode] = {
		list.map(node => {
			if(node.receives(direction))
				Some(node)
			else
				node.next(direction)	
		}).collect({case Some(x) => x})
	}
}

class CompositionNetwork[A <: Actor with Composable](comb: ArrowOperator[A]) extends Actor {
	import CompositionNetwork._
	
	val (nrOfPaths, startNodes, endNodes) = CompositionNetwork.fromArrowOperator(comb, self, context)
	val incomingNodes = filterNodes(startNodes, Incoming)
	val outgoingNodes = filterNodes(endNodes, Outgoing)
	
	assert(!(startNodes == Nil && endNodes == Nil))
	
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