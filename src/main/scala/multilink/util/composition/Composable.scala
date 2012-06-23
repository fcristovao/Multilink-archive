package multilink.util.composition

import akka.actor.{Actor, FSM, ActorRef, Props, ActorLogging}
import akka.actor.Actor._
import akka.actor.ActorRefFactory

trait Composable

object Composable {

	implicit def byNameActorToArrowOperator[A <: Actor with Composable : Manifest](x: => A): ArrowOperator[A] = {
		Lift(() => x)
	}
	
	implicit def arrowOp2Actor[A <: Actor with Composable](comb: ArrowOperator[A]): Actor = {
		new CompositionNetwork(comb)
	}
}
