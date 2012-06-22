package multilink.util.composition

import akka.actor.{Actor, FSM, ActorRef, Props, ActorLogging}
import akka.actor.Actor._

sealed trait ArrowOperator{
	def lift: ArrowOperator
	def >>>(other: ArrowOperator): ArrowOperator
	def &&&(other: ArrowOperator): ArrowOperator 
	
	def incoming: Boolean
	def outgoing: Boolean
	
	def alsoOutgoing: ArrowOperator 
	def onlyOutgoing: ArrowOperator 
}

case class Lift (val actorFactory: () => Actor with Composable, override val incoming: Boolean = true, override val outgoing: Boolean = false) extends ArrowOperator {
	def lift = this
	
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
	def lift = this
	
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
	def lift = this
	
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
