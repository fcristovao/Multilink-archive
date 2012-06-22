package multilink.util

import akka.actor.{Actor, FSM, ActorRef, Props, ActorLogging}
import akka.actor.Actor._

import multilink.util.composition._
import multilink.util.composition.Composable._

import scala.collection.mutable

private[util] class Dispatcher private(final val nrOfPaths: Int, val startNodes: List[CompositionNode], val endNodes: List[CompositionNode]) extends Actor{
	private def this(nrOfPaths: Int, tupleOfNodesList: (List[CompositionNode], List[CompositionNode])) = this(nrOfPaths, tupleOfNodesList._1, tupleOfNodesList._2)
	private def this(intermediate: List[(Option[CompositionNode], Option[CompositionNode])]) = this(intermediate.size, intermediate.foldLeft((List[CompositionNode](),List[CompositionNode]()))((tupleList, tupleOption) => (tupleOption._1.toList ::: tupleList._1 ,  tupleOption._2.toList ::: tupleList._2)))
	def this(comb: ArrowOperator) = this(Composable.toCompositionNodeList(comb))
	
	assert(!(startNodes == Nil && endNodes == Nil)) //this should be impossible, so we make sure of it
	
	private sealed abstract class State(val sender: ActorRef, var pathsCompleted: Int = 0, var replies: List[Any] = List())
	private case class CommonState(override val sender: ActorRef) extends State(sender)
	private case class ExtendedState(override val sender: ActorRef, val processMsgReceived: Process) extends State(sender)
	
	/*
	 * expectedNrMsgs: Expected Number of messages to reach the beginning of the path (with the Outgoing direction, that is, from the end to the beginning) 
	 * replies: accumulated reply messages along this path. When the beginning of path is reached, these are added to the global replies
	 * nrMsgsSoFar: number of messages that reached the beginning of the path so far
	 */
	private case class ReplyingState(var expectedNrMsgs: Int, var replies: List[Any], var nrMsgsSoFar: Int = 0)
	
	
	override def preStart() = { 
		val alreadyCreated = scala.collection.mutable.Map[Props, ActorRef]()
		
		def starter(chain: Option[CompositionNode], direction: Direction): Unit = {
			chain match {
				case None => //nothing to do;
				case Some(node) => {
					val actorRef = alreadyCreated.get(node.actorProps) match {
						case None => {
							val tmp = context.actorOf(node.actorProps)
							alreadyCreated += (node.actorProps -> tmp)
							tmp
						}
						case Some(x) => x
					} 

					node.actorRef = actorRef
					
					direction match {
						case Incoming => starter(node.next, Incoming)
						case Outgoing => starter(node.previous, Outgoing)
					}
				}
			}
		}
		startNodes.foreach(node => starter(Some(node), Incoming))
		endNodes.foreach(node => starter(Some(node), Outgoing))
	}
	
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
				val state = pendentSenders(generation)
				direction match {
					case Incoming => {
						fromNode.next match {
							case Some(node) => {
								node.actorRef ! Process(generation, node, direction, msg)
							}
							case None => {
								//We've reached the end of a path with a Done message, so no outgoing path has to be followed (because there were no replies along this path)
								state.pathsCompleted += 1
								tryToAnswer(generation, state)
							}
						}
					}
					case Outgoing => {
						fromNode.previous match {
							case None => { // There's no more nodes outgoing in this path
								val replyingState = awaitingPathReplies((generation, fromNode.pathNr))
								replyingState.nrMsgsSoFar += 1
								if(replyingState.nrMsgsSoFar == replyingState.expectedNrMsgs){
									state.pathsCompleted += 1
									state.replies = state.replies ++ replyingState.replies 
									awaitingPathReplies -= ((generation, fromNode.pathNr))
									tryToAnswer(generation, state)
								}
							}
							case Some(node) => {
								node.actorRef ! Process(generation, node, direction, msg) 
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
				val state =  ExtendedState(sender, processMsg)
				pendentSenders += nextGen -> state
						
				direction match {
					case Incoming => {
						if(!startNodes.isEmpty) {
							state.pathsCompleted = nrOfPaths - startNodes.size
							startNodes.foreach(node => node.actorRef ! Process(nextGen, node, direction, msg))
						} else {
							sender ! Done(generationFromFather, thisNode, direction, msg)
						}
					}
					case Outgoing => {
						if(!startNodes.isEmpty) {
							state.pathsCompleted = nrOfPaths - endNodes.size
							endNodes.foreach(node => {awaitingPathReplies += (nextGen, node.pathNr) -> ReplyingState(1, Nil); node.actorRef ! Process(nextGen, node, direction, msg) })
						} else {
							sender ! Done(generationFromFather, thisNode, direction, msg)
						}
					}
				}
			}
		
		case anyOtherMsg => {
			if(!startNodes.isEmpty) {
				val nextGen = generation.next()
				pendentSenders += nextGen -> CommonState(sender)
				
				startNodes.foreach(node => node.actorRef ! Process(nextGen, node, Incoming, anyOtherMsg))
			}
		}
	}
}
