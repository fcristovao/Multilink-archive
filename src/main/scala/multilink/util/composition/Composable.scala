package multilink.util.composition

import akka.actor.{Actor, FSM, ActorRef, Props, ActorLogging}
import akka.actor.Actor._

import multilink.util.Dispatcher

trait Composable

object Composable {
	sealed abstract class Messages(generation: Int, who: CompositionNode, direction: Direction) 
	case class Process(generation: Int, who: CompositionNode, direction: Direction, msg: Any) extends Messages(generation, who, direction)
	case class Done(generation: Int, who: CompositionNode, direction: Direction, msg: Any) extends Messages(generation, who, direction)
	case class Reply(generation: Int, who: CompositionNode, direction: Direction, replies: List[Any]) extends Messages(generation, who, direction)
	
	sealed trait Direction 
	case object Incoming extends Direction
	case object Outgoing extends Direction
		
	private[util] case class CompositionNode(val actorProps: Props, val pathNr: Int = 0, val previous: Option[CompositionNode] = None, var next: Option[CompositionNode] = None, var actorRef: ActorRef = null){
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
	def toCompositionNodeList(comb: ArrowOperator): List[(Option[CompositionNode], Option[CompositionNode])] = {
		val alreadyCreated = scala.collection.mutable.Map[() => Actor, Props]()
		
		def helper(comb: ArrowOperator, pathNr: Int = 0, previous: Option[CompositionNode] = None): (Option[CompositionNode], Option[CompositionNode]) = {
			comb match {
				case Lift(actorFactory, inbound, outbound) => {
					val actorProps = 
						alreadyCreated.get(actorFactory) match {
							case None => {
								val tmp = Props(actorFactory)
								alreadyCreated += (actorFactory -> tmp)
								tmp
							}
							case Some(x) => {
								x
							}
						}
					
					val result = Some(CompositionNode(actorProps, pathNr, previous))
					(inbound, outbound) match {
						case (true, true) => (result, result)
						case (false, true) => (None, result)
						case (true, false) => (result, previous)
						//case (false, false) => Should never happen, so if it does, let it be signalled with the exception
					}
				}
				case Composition(composables) => {
					def anotherHelper(listOfComposables: List[ArrowOperator], previous: Option[CompositionNode]): (Option[CompositionNode], Option[CompositionNode]) = listOfComposables match {
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
					val actorProps = Props(new Dispatcher(sp))
					val result = Some(CompositionNode(actorProps, pathNr, previous))

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
	
	implicit def byNameActorToArrowOperator(x: => Actor with Composable): ArrowOperator = {
		Lift(() => x)
	}
	
	implicit def arrowOp2Actor(comb: ArrowOperator) : Actor = {
		new Dispatcher(comb)
	}
	
}
