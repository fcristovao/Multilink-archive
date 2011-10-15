package multilink.util

import akka.actor.{Actor, FSM, ActorRef}
import akka.actor.Actor._
import akka.event.EventHandler

trait TCombinable{
	def >>>(other: TCombinable): TCombinable
}

trait Combinable //I actually needed a trait named composable, but also needed a class named that way, so combinable it is


//This class should be named Arr(ow), as it corresponds to the Lift operation in Arrows
class Composable[A <: Combinable with Actor: Manifest] private (private val actorClass: Class[_ <: Combinable with Actor]) extends TCombinable{
	
	def this() = this(manifest[A].erasure.asInstanceOf[Class[_ <: Combinable with Actor]])
	
	def >>>(other: TCombinable): TCombinable = {
		other match {
			case x: Composable[_] => Composition(List(this, x))
			case x: Composition => Composition(this :: x.combinables)
		}
	}

	override def toString(): String = {
		"⇑("+actorClass.getCanonicalName().split('.').last+")"
	}
}

case class Composition(combinables: List[TCombinable]) extends TCombinable {
	def >>>(other: TCombinable): TCombinable = {
		other match {
			case x: Composable[_] => Composition(combinables :+ x)
			case x: Composition => Composition(this :: x.combinables)
		}
	}
	
	override def toString(): String = {
		combinables mkString " >>> "
	}
}


object Composable {
	
	sealed trait ComposableMessages
	case object Done extends ComposableMessages
	case class Reply(msg: Any) extends ComposableMessages 
	
	private class Dispatcher(val actorClasses: List[Class[_ <: Combinable with Actor]]) extends Actor {
	
		val linked = actorClasses.map(actorOf(_))
		
		override def preStart() = {
			linked foreach (_.start())
		}
		
		def receive = {
			case msg => 
				val sender = self.sender.get
				//println(sender)
				linked takeWhile ((actor) => {
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
	
	
	implicit def comb2ActorRef(comb: TCombinable) : ActorRef = {
		actorOf(comb2Actor(comb))
	}
	
	implicit def comb2Actor(comb: TCombinable) : Actor = {
		def toClassList(comb: TCombinable) : List[Class[_ <: Combinable with Actor]] = {
			comb match {
				case x: Composable[_] => List(x.actorClass)
				case x: Composition => x.combinables flatMap(toClassList(_))
			}
		}
		new Dispatcher(toClassList(comb))
	}
	
}



