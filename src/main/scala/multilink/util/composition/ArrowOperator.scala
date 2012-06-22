package multilink.util.composition

import akka.actor.{Actor, FSM, ActorRef, Props, ActorLogging}
import akka.actor.Actor._

sealed trait ArrowOperator[-A <: Composable]{
	def lift: ArrowOperator[A]
	def >>>[B <: A](other: ArrowOperator[B]): ArrowOperator[B]
	def &&&[B <: A](other: ArrowOperator[B]): ArrowOperator[B] 
	
	def incoming: Boolean
	def outgoing: Boolean
	
	def alsoOutgoing: ArrowOperator[A] 
	def onlyOutgoing: ArrowOperator[A] 
}

case class Lift[A] (val actorFactory: () => Composable, override val incoming: Boolean = true, override val outgoing: Boolean = false) extends ArrowOperator[A] {
	def lift = this
	
	def >>>[B <: A](other: ArrowOperator[B]): ArrowOperator[B] = {
		other match {
			case Lift(_,_,_) | Splitter(_) => Composition[B](List(this, other))
			case Composition(composables) => Composition(this :: composables)
		}
	}
	
	def &&&[B <: A](other: ArrowOperator[B]): ArrowOperator[B] = {
		Splitter(List(this, other))
	}
	
	def alsoOutgoing: ArrowOperator[A] = copy(incoming = true, outgoing = true)
	def onlyOutgoing: ArrowOperator[A] = copy(incoming = false, outgoing = true)

	override def toString(): String = {
		"⇑("+actorFactory+")"
	}
}

case class Composition[A](composables: List[ArrowOperator[A]]) extends ArrowOperator[A] {
	def lift = this
	
	def >>>[B <: A](other: ArrowOperator[B]): ArrowOperator[B] = {
		other match {
			case Lift(_,_,_) | Splitter(_) => Composition(composables :+ other)
			case Composition(otherComposables) => Composition(composables ::: otherComposables)
		}
	}
	def &&&[B <: A](other: ArrowOperator[B]): ArrowOperator[B] = {
		Splitter(List(this, other))
	}
	
	lazy override val incoming: Boolean = composables.exists(_.incoming) 
	lazy override val outgoing: Boolean = composables.exists(_.outgoing)
	
	def alsoOutgoing: ArrowOperator[A] = copy(composables.map(_.alsoOutgoing))
	def onlyOutgoing: ArrowOperator[A] = copy(composables.map(_.onlyOutgoing))
	
	override def toString(): String = {
		"("+(composables mkString " >>> ")+")"
	}
}
case class Splitter[A](splittedInto: List[ArrowOperator[A]]) extends ArrowOperator[A] {
	def lift = this
	
	def >>>[B <:A](other: ArrowOperator[B]): ArrowOperator[B] = {
		other match {
			case Lift(_,_,_) | Splitter(_) => Composition(List(this, other))
			case Composition(composables) => Composition(this :: composables)
		}
	}
	def &&&[B <: A](other: ArrowOperator[B]): ArrowOperator[B] = {
		other match {
			case Lift(_,_,_) | Composition(_) => Splitter(splittedInto :+ other)
			case Splitter(otherSplittedInto) => Splitter(splittedInto ::: otherSplittedInto)
		}
	}
	
	lazy val incoming: Boolean = splittedInto.exists(_.incoming) 
	lazy val outgoing: Boolean = splittedInto.exists(_.outgoing)
	
	def alsoOutgoing: ArrowOperator[A] = copy(splittedInto = splittedInto.map(_.alsoOutgoing))
	def onlyOutgoing: ArrowOperator[A] = copy(splittedInto = splittedInto.map(_.onlyOutgoing))
	
	override def toString(): String = {
		"("+(splittedInto mkString " &&& ")+")"
	}
}
