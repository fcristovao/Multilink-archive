package multilink.util

import akka.actor.{Actor, FSM, ActorRef}
import akka.actor.Actor._
import akka.event.EventHandler

import akka.config.Config.config

trait Composable

object Composable {
	import Dispatcher._
	
	sealed trait ArrowOperator{
		def >>>(other: ArrowOperator): ArrowOperator
		def &&&(other: ArrowOperator): ArrowOperator 
		
		def incoming: Boolean
		def outgoing: Boolean
		
		def alsoOutgoing: ArrowOperator 
		def onlyOutgoing: ArrowOperator 
	}
	
	case class Lift (val actorFactory: () => Actor with Composable, override val incoming: Boolean = true, override val outgoing: Boolean = false) extends ArrowOperator {
		def >>>(other: ArrowOperator): ArrowOperator = {
			other match {
				case Lift(_,_,_) | Splitter(_) => Composition(List(this, other))
				case Composition(composables) => Composition(this :: composables)
			}
		}
		
		def &&&(other: ArrowOperator): ArrowOperator = {
			Splitter(List(this, other))
		}
		
		def alsoOutgoing: ArrowOperator = copy(incoming = true, outgoing = true)
		def onlyOutgoing: ArrowOperator = copy(incoming = false, outgoing = true)
	
		override def toString(): String = {
			"⇑("+actorFactory+")"
		}
	}
	
	case class Composition(composables: List[ArrowOperator]) extends ArrowOperator {
		def >>>(other: ArrowOperator): ArrowOperator = {
			other match {
				case Lift(_,_,_) | Splitter(_) => Composition(composables :+ other)
				case Composition(otherComposables) => Composition(composables ::: otherComposables)
			}
		}
		
		def &&&(other: ArrowOperator): ArrowOperator = {
			Splitter(List(this, other))
		}
		
		lazy override val incoming: Boolean = composables.exists(_.incoming) 
		lazy override val outgoing: Boolean = composables.exists(_.outgoing)
		
		def alsoOutgoing: ArrowOperator = Composition(composables.map(_.alsoOutgoing))
		def onlyOutgoing: ArrowOperator = Composition(composables.map(_.onlyOutgoing))
		
		override def toString(): String = {
			"("+(composables mkString " >>> ")+")"
		}
	}
	
	case class Splitter(splittedInto: List[ArrowOperator]) extends ArrowOperator {
		def >>>(other: ArrowOperator): ArrowOperator = {
			other match {
				case Lift(_,_,_) | Splitter(_) => Composition(List(this, other))
				case Composition(composables) => Composition(this :: composables)
			}
		}
		
		def &&&(other: ArrowOperator): ArrowOperator = {
			other match {
				case Lift(_,_,_) | Composition(_) => Splitter(splittedInto :+ other)
				case Splitter(otherSplittedInto) => Splitter(splittedInto ::: otherSplittedInto)
			}
		}
		
		lazy val incoming: Boolean = splittedInto.exists(_.incoming) 
		lazy val outgoing: Boolean = splittedInto.exists(_.outgoing)
		
		def alsoOutgoing: ArrowOperator = copy(splittedInto = splittedInto.map(_.alsoOutgoing))
		def onlyOutgoing: ArrowOperator = copy(splittedInto = splittedInto.map(_.onlyOutgoing))
		
		override def toString(): String = {
			"("+(splittedInto mkString " &&& ")+")"
		}
	}
	
	//needed for Lift(<someActor>)
	implicit def byNameActorToFunc0(x: => Actor with Composable): () => Actor with Composable = {
		() => x
	}
	
	implicit def byNameActorToArrowOperator(x: => Actor with Composable): ArrowOperator = {
		Lift(() => x)
	}
	
	
	private[util] object Dispatcher {
		
		sealed abstract class DispatcherMessages(generation: Int, who: Dispatcher.DispatcherNode, direction: Direction) 
		case class Process(generation: Int, who: Dispatcher.DispatcherNode, direction: Direction, msg: Any) extends DispatcherMessages(generation, who, direction)
		case class Done(generation: Int, who: Dispatcher.DispatcherNode, direction: Direction, msg: Any) extends DispatcherMessages(generation, who, direction)
		case class Reply(generation: Int, who: Dispatcher.DispatcherNode, direction: Direction, replies: List[Any]) extends DispatcherMessages(generation, who, direction)
		
		sealed trait Direction 
		case object Incoming extends Direction
		case object Outgoing extends Direction
		
		private[util] case class DispatcherNode(val actorRef: ActorRef, val pathNr: Int = 0, val previous: Option[DispatcherNode] = None, var next: Option[DispatcherNode] = None){
			override def toString(): String = {
				return "∆("+actorRef+")" + (if(!next.isEmpty) ("⇄"+next.get.toString()) else "")
			}
		}
		
		/*
		 * This version creates only ONE actor instance in the case of the *same* lift is found in the composition.
		 * E.g.:
		 * val tmp = Lift(new Firewall())
		 * tmp >>> new Gateway >>> tmp
		 * ^-- This would create only one Firewall instance (although it would receive the same message twice)
		 */
		private def toDispatcherNodeList(comb: ArrowOperator): List[(Option[DispatcherNode], Option[DispatcherNode])] = {
			val alreadyCreated = scala.collection.mutable.Map[() => Actor, ActorRef]()
			
			def helper(comb: ArrowOperator, pathNr: Int = 0, previous: Option[DispatcherNode] = None): (Option[DispatcherNode], Option[DispatcherNode]) = {
				comb match {
					case Lift(actorFactory, inbound, outbound) => {
						val actorRef = 
							alreadyCreated.get(actorFactory) match {
								case None => {
									val tmp = actorOf(actorFactory())
									alreadyCreated += (actorFactory -> tmp)
									tmp
								}
								case Some(x) => {
									x
								}
							}
						
						val result = Some(DispatcherNode(actorRef, pathNr, previous))
						(inbound, outbound) match {
							case (true, true) => (result, result)
							case (false, true) => (None, result)
							case (true, false) => (result, previous)
							//case (false, false) => Should never happen, so if it does, let it be signalled with the exception
						}
					}
					case Composition(composables) => {
						def anotherHelper(listOfComposables: List[ArrowOperator], previous: Option[DispatcherNode]): (Option[DispatcherNode], Option[DispatcherNode]) = listOfComposables match {
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
						anotherHelper(composables, previous)
					}
					case sp @ Splitter(splitted) => {
						val actorRef = actorOf(new Dispatcher((splitted zip (0 until splitted.size)).map({case (comb, pathNr) => helper(comb, pathNr)})))
						val result = Some(DispatcherNode(actorRef, pathNr, previous))

						(sp.incoming, sp.outgoing) match {
							case (true, true) => (result, result)
							case (false, true) => (None, result)
							case (true, false) => (result, previous)
							//case (false, false) => Should never happen, so if it does, let it be signalled with the exception
						}
					}
				}
			}
			
			comb match {
				case Splitter(splitted) => {
					(splitted zip (0 until splitted.size)).map({case (comb, pathNr) => helper(comb, pathNr)})
				}
				case anyOther => List(helper(comb))
			}
		}
	}
	
	private class Dispatcher private(final val nrOfPaths: Int, val startNodes: List[Dispatcher.DispatcherNode], val endNodes: List[Dispatcher.DispatcherNode]) extends Actor{
		assert(!(startNodes == Nil && endNodes == Nil)) //this should be impossible, so we make sure of it
		
		import Dispatcher._
		import scala.collection.mutable
		
		private sealed abstract class State(val sender: Option[ActorRef], var pathsCompleted: Int = 0, var replies: List[Any] = List())
		private case class CommonState(override val sender: Option[ActorRef]) extends State(sender)
		private case class ExtendedState(override val sender: Option[ActorRef], val processMsgReceived: Process) extends State(sender)
		
		/*
		 * expectedNrMsgs: Expected Number of messages to reach the beginning of the path (with the Outgoing direction, that is, from the end to the beginning) 
		 * replies: accumulated reply messages along this path. When the beginning of path is reached, these are added to the global replies
		 * nrMsgsSoFar: number of messages that reached the beginning of the path so far
		 */
		private case class ReplyingState(var expectedNrMsgs: Int, var replies: List[Any], var nrMsgsSoFar: Int = 0)
		
		private def this(nrOfPaths: Int, tupleOfNodesList: (List[DispatcherNode], List[DispatcherNode])) = this(nrOfPaths, tupleOfNodesList._1, tupleOfNodesList._2)
		private def this(intermediate: List[(Option[DispatcherNode], Option[DispatcherNode])]) = this(intermediate.size, intermediate.foldLeft((List[DispatcherNode](),List[DispatcherNode]()))((tupleList, tupleOption) => (tupleOption._1.toList ::: tupleList._1 ,  tupleOption._2.toList ::: tupleList._2)))
		def this(comb: ArrowOperator) = this(Dispatcher.toDispatcherNodeList(comb))
		
		override def preStart() = { 
			// The .start() method in ActorRef is idempotent, so there's no problem in calling it twice (if it wasn't, one could easily use a set)
			def starter(chain: Option[DispatcherNode], direction: Direction): Unit = {
				chain match {
					case None => //nothing to do;
					case Some(node) => {
						node.actorRef.start(); 
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
				if(state.sender.isEmpty){
					EventHandler.info(this, "No sender in scope, can't reply"+ (if(!state.replies.isEmpty){" (reply(ies) would be ["+ (state.replies mkString "], [")+"])"} else {""})+".")
				} else {
					state match {
						case ExtendedState(sender, Process(receivedGeneration, node, direction, msg)) => {
							state.replies match {
							  case Nil => sender.get ! Done(receivedGeneration, node, direction, msg)
							  case listOfReplies => sender.get ! Reply(receivedGeneration, node, direction, listOfReplies)
							}
						}
						case state: CommonState => {
							state.replies foreach (state.sender.get ! _)
						}
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
					val state =  ExtendedState(self.sender, processMsg)
					pendentSenders += nextGen -> state
							
					direction match {
						case Incoming => {
							if(!startNodes.isEmpty) {
								state.pathsCompleted = nrOfPaths - startNodes.size
								startNodes.foreach(node => node.actorRef ! Process(nextGen, node, direction, msg))
							} else {
								self.reply(Done(generationFromFather, thisNode, direction, msg))
							}
						}
						case Outgoing => {
							if(!startNodes.isEmpty) {
								state.pathsCompleted = nrOfPaths - endNodes.size
								endNodes.foreach(node => {awaitingPathReplies += (nextGen, node.pathNr) -> ReplyingState(1, Nil); node.actorRef ! Process(nextGen, node, direction, msg) })
							} else {
								self.reply(Done(generationFromFather, thisNode, direction, msg))
							}
						}
					}
				}
			
			case anyOtherMsg => {
				if(!startNodes.isEmpty) {
					val nextGen = generation.next()
					pendentSenders += nextGen -> CommonState(self.sender)
					
					startNodes.foreach(node => node.actorRef ! Process(nextGen, node, Incoming, anyOtherMsg))
				}
			}
		}
	}
	
	private trait LoggableDispatcher extends Dispatcher {
	
		val debugMsg = config.getBool("akka.actor.debug.receive", false)
		
		override abstract def receive : Receive = {
			case msg => {
				if(debugMsg){
					EventHandler.debug(this, "received message: "+ msg)
				}
				super.receive(msg)
			}
		}
			
	}
	
	implicit def arrowOp2Actor(comb: ArrowOperator) : Actor = {
		new Dispatcher(comb)
	}
}
