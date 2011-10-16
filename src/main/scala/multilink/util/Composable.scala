package multilink.util

import akka.actor.{Actor, FSM, ActorRef}
import akka.actor.Actor._
import akka.event.EventHandler

trait Composable

object Composable {
	sealed trait ComposableMessages
	case object Done extends ComposableMessages
	case class Reply(msg: Any) extends ComposableMessages 
	
	trait ArrowOperator{
		def >>>(other: ArrowOperator): ArrowOperator
	}
	
	case class Lift (val actorFactory: () => Actor with Composable) extends ArrowOperator{
		
		def >>>(other: ArrowOperator): ArrowOperator = {
			other match {
				case x @ Lift(_) => Composition(List(this, x))
				case Composition(composables) => Composition(this :: composables)
			}
		}
	
		override def toString(): String = {
			"⇑("+actorFactory+")"
		}
	}
	
	//needed for Lift(<someActor>)
	implicit def byNameActorToFunc0(x: => Actor with Composable): () => Actor with Composable = {
		() => x
	}
	
	implicit def byNameActorToArrowOperator(x: => Actor with Composable): ArrowOperator = {
		new Lift(() => x)
	}
	
	case class Composition(composables: List[ArrowOperator]) extends ArrowOperator {
		def >>>(other: ArrowOperator): ArrowOperator = {
			other match {
				case x: Lift => Composition(composables :+ x)
				case x: Composition => Composition(this :: x.composables)
			}
		}
		
		override def toString(): String = {
			composables mkString " >>> "
		}
	}
	
	
	private object Dispatcher {
		/*
		 * This version creates different actor instances in the case of the *same* lift is found in the composition.
		 * E.g.:
		 * val tmp = Lift(new Firewall())
		 * tmp >>> new Gateway >>> tmp
		 * ^-- This would create 2 different Firewall instances
		 */
		/*
		def toListActorRef(comb: ArrowOperator) : List[ActorRef] = {
			comb match {
				case Lift(actorFactory) => List(actorOf(actorFactory()))
				case Composition(composables) => composables flatMap(toListActorRef(_))
			}
		}
		*/
		
		/*
		 * This version creates only ONE actor instance in the case of the *same* lift is found in the composition.
		 * E.g.:
		 * val tmp = Lift(new Firewall())
		 * tmp >>> new Gateway >>> tmp
		 * ^-- This would create only one Firewall instance (although it would receive the same message twice)
		 */
		def toListActorRef(comb: ArrowOperator) : List[ActorRef] = {
			def helper(comb: ArrowOperator, listSoFar: List[ActorRef] = List(), alreadyCreated: Map[Lift,ActorRef] = Map()): (List[ActorRef], Map[Lift,ActorRef]) = {
				comb match {
					case l @ Lift(actorFactory) => {
						alreadyCreated.get(l) match {
							case None => {
								val actorRef = actorOf(actorFactory())
								(listSoFar :+ actorRef, alreadyCreated + (l -> actorRef))
							}
							case Some(actorRef) => {
								(listSoFar :+ actorRef, alreadyCreated)
							}
						}
						
					}
					case Composition(composables) => {
						composables.foldLeft((listSoFar, alreadyCreated)){
							(tuple, arrowOperator) =>
								tuple match {
									case (listSoFar, alreadyCreated) => helper(arrowOperator, listSoFar, alreadyCreated)
								}
						}
					}
				}
			}
			
			helper(comb)._1
		}
	}
	
	private class Dispatcher(val actorRefs: List[ActorRef]) extends Actor {
		
		def this(comb: ArrowOperator) = this(Dispatcher.toListActorRef(comb))
		
		override def preStart() = {
			actorRefs foreach (_.start())
		}
		
		def receive = {
			case msg => 
				val sender = self.sender.get
				//println(sender)
				actorRefs takeWhile ((actor) => {
					(actor ? msg).as[ComposableMessages] match {
						case Some(Done) => true
						case Some(Reply(msg)) => sender ! msg; false
						case Some(msg) => //the actor answered something that it shouldn't
							EventHandler.error(this, "Actor "+ actor+" answered something that it shouldn't: "+msg+".") 
							false
						case None => { //the actor has timed-out (either it didn't answer, or it is really dead)
							EventHandler.error(this, "Actor "+ actor+" didn't answer in time.") 
							false
						}
					}
				})
		}
	}
	
	
	implicit def arrowOp2ActorRef(comb: ArrowOperator) : ActorRef = {
		actorOf(arrowOp2Actor(comb))
	}
	
	implicit def arrowOp2Actor(comb: ArrowOperator) : Actor = {
		new Dispatcher(comb)
	}
}



