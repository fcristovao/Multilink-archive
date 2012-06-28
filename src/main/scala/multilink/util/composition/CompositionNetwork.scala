package multilink.util.composition

import Composable._
import akka.actor.{Actor, ActorRef, ActorRefFactory, Props}
import scala.collection.mutable


object CompositionNetwork{

	sealed abstract class Messages(generation: Int, who: CompositionNetwork, direction: Direction) 
	case class Process(generation: Int, who: CompositionNetwork, direction: Direction, msg: Any) extends Messages(generation, who, direction)
	case class Done(generation: Int, who: CompositionNetwork, direction: Direction, msg: Any) extends Messages(generation, who, direction)
	
	sealed trait Direction 
	case object Incoming extends Direction
	case object Outgoing extends Direction
	
	private[CompositionNetwork] class NodeHelperDispatcher() extends Actor {
		def receive() = {
			case CompositionNetwork.Process(generation, node, direction, msg) =>
				(direction match {
					case Incoming => node.incomingNodes
					case Outgoing => node.outgoingNodes
				}).foreach(node => sender ! CompositionNetwork.Process(generation, node, direction, msg))
		}
	}
	
	/*
	 * This version creates only ONE actor instance in the case of the *same* lift is found in the composition.
	 * E.g.:
	 * val tmp = Lift(new Firewall())
	 * tmp >>> new Gateway >>> tmp
	 * ^-- This would create only one Firewall instance (although it would receive the same message twice)
	 */
	def fromArrowOperator[A <: Actor with Composable](comb: ArrowOperator[A], actorRefFactory: ActorRefFactory): CompositionNetwork = {
		val alreadyCreated = scala.collection.mutable.Map[() => Actor with Composable, ActorRef]()
		var nameCounter = 1

		def setParents(node: CompositionNetwork, parent: Option[CompositionNetwork] = None) : CompositionNetwork = {
			node.parent = parent
			node match {
				case _: LiftNode => {
					//Nothing to do
				}
				case anythingElse => { //CompNode or SplitterNode
					for(node <- anythingElse.startNodes){
						var current : Option[CompositionNetwork] = Some(node)
						while(current != None){
							current.get.parent = parent
							setParents(current.get, Some(anythingElse))
							current = current.get.next
						}
					}
				}
			}
			node
		}
		
		def helper(comb: ArrowOperator[A], pathNr: Int = 1, previous: Option[CompositionNetwork] = None): CompositionNetwork = {
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
					
					new LiftNode(lifted.actorName, actorRef, inbound, outbound, pathNr, previous)
				}
				case comp @ Composition(composables) => {
					val actorRef = actorRefFactory.actorOf(Props[NodeHelperDispatcher], nameCounter + "-Comp")
					nameCounter+=1
					
					def anotherHelper(listOfComposables: List[ArrowOperator[A]], previous: Option[CompositionNetwork]): (Option[CompositionNetwork], Option[CompositionNetwork]) = {
						listOfComposables match {
							case head :: tail => {
								val result = Some(helper(head, pathNr, previous))
								val (next, newestPrevious) = anotherHelper(tail, result)
								result.get.next = next
								(result, newestPrevious)
							}
							case Nil => (None, previous)
						}
					}
					val (startNode, endNode) = anotherHelper(composables, previous)
					
					new CompositionNode(startNode.get, endNode.get, "Comp", actorRef, comp.incoming, comp.outgoing, pathNr, previous)
					
				}
				case sp @ Splitter(splitted) => {
					val actorRef = actorRefFactory.actorOf(Props[NodeHelperDispatcher], nameCounter + "-Splitted")
					nameCounter+=1
					
					val paths = (splitted zip (1 until splitted.size+1)).map({case (comb, pathNr) => helper(comb, pathNr)})
					
					new SplitterNode(paths, "Splitted", actorRef, sp.incoming, sp.outgoing, pathNr, previous)
				}
			}
		}
			
		val rootOfNetwork = helper(comb)
		
		setParents(rootOfNetwork)
	}
}


private[util] abstract class CompositionNetwork(
		val nrOfPaths: Int,
		val actorName: String,
		val actorRef: ActorRef,
		val receiveIncoming: Boolean,
		val receiveOutgoing: Boolean,
		val pathNr: Int = 1, 
		val previous: Option[CompositionNetwork] = None, 
		var next: Option[CompositionNetwork] = None,
		var parent: Option[CompositionNetwork] = None) {
	
	import scala.collection.immutable
	import CompositionNetwork._

	def startNodes: List[CompositionNetwork]
	def endNodes: List[CompositionNetwork]
	
	def receives(direction: Direction) = direction match {case Incoming => receiveIncoming; case Outgoing => receiveOutgoing}
	
	private lazy val nextMap = immutable.Map[Direction,Option[CompositionNetwork]](Incoming -> _next(Incoming), Outgoing-> _next(Outgoing)) 
	
	private def _next(direction: Direction): Option[CompositionNetwork] = {
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
	
	//memoized version of _next
	def next(direction: Direction): Option[CompositionNetwork] = nextMap(direction)
	
	private def filterNodes(list: List[CompositionNetwork], direction: Direction): List[CompositionNetwork] = {
		list.map(node => {
			if(node.receives(direction))
				Some(node)
			else
				node.next(direction)	
		}).collect({case Some(x) => x})
	}
	
	lazy val incomingNodes = filterNodes(startNodes, Incoming)
	lazy val outgoingNodes = filterNodes(endNodes, Outgoing)
}
	
private[util] class LiftNode(
		actorName: String,
		actorRef: ActorRef,
		receiveIncoming: Boolean,
		receiveOutgoing: Boolean,
		pathNr: Int = 1, 
		previous: Option[CompositionNetwork] = None, 
		next: Option[CompositionNetwork] = None,
		parent: Option[CompositionNetwork] = None) extends CompositionNetwork(1, actorName, actorRef, receiveIncoming, receiveOutgoing, pathNr, previous, next, parent){
	
	val startNodes: List[CompositionNetwork] = List(this)
	val endNodes: List[CompositionNetwork] = List(this)
	
	override def toString = "LiftNode"
}

private[util] class CompositionNode(
		startNode: CompositionNetwork,
		endNode: CompositionNetwork,
		actorName: String,
		actorRef: ActorRef,
		receiveIncoming: Boolean,
		receiveOutgoing: Boolean,
		pathNr: Int = 1, 
		previous: Option[CompositionNetwork] = None, 
		next: Option[CompositionNetwork] = None,
		parent: Option[CompositionNetwork] = None) extends CompositionNetwork(1, actorName, actorRef, receiveIncoming, receiveOutgoing, pathNr, previous, next, parent){
	
	val startNodes: List[CompositionNetwork] = List(startNode)
	val endNodes: List[CompositionNetwork] = List(endNode)
	
	override def toString = "CompositionNode"
}
	
private[util] class SplitterNode(
		paths: List[CompositionNetwork],
		actorName: String,
		actorRef: ActorRef,
		receiveIncoming: Boolean,
		receiveOutgoing: Boolean,
		pathNr: Int = 1, 
		previous: Option[CompositionNetwork] = None, 
		next: Option[CompositionNetwork] = None,
		parent: Option[CompositionNetwork] = None) extends CompositionNetwork(paths.size, actorName, actorRef, receiveIncoming, receiveOutgoing, pathNr, previous, next, parent){
	
	assert(paths != Nil)
	
	val startNodes: List[CompositionNetwork] = paths
	val endNodes: List[CompositionNetwork] = paths
	
	override def toString = "SplitterNode"
}
	
class CompositionDispatcher[A <: Actor with Composable](comb: ArrowOperator[A]) extends Actor {
	import CompositionNetwork._
	
	val rootCompositionNetwork = CompositionNetwork.fromArrowOperator(comb, context)
	
	private val generation = Iterator from 0
	private val proxiesMap = mutable.Map[ActorRef, ActorRef]()
	private val proxiesID = Iterator from 1
	
	def receive() = {
		case msg => {
			val nextGen = generation.next()
			val tmp = sender // one has to use this because getOrElseUpdate uses by name arguments
			val proxy = proxiesMap.getOrElseUpdate(tmp,context.actorOf(Props(new ProxyActor(tmp)),"ProxyNr"+proxiesID.next()))

			proxy ! CompositionNetwork.Process(nextGen, rootCompositionNetwork, Incoming, msg)
		}
	}
	
}

