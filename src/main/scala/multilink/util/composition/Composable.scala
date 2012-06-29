package multilink.util.composition

import akka.actor.{Actor, FSM, ActorRef, Props, ActorLogging}
import akka.actor.Actor._
import akka.actor.ActorRefFactory
import akka.actor.ScalaActorRef

trait Composable

object Composable {

	implicit def byNameActorToArrowOperator[A <: Actor with Composable](x: => A)(implicit actorRefFactory: ActorRefFactory, manifest: Manifest[A]): Lift[A] = {
		Lift[A](() => x, actorRefFactory, manifest)
	}
	
	implicit def arrowOp2Actor[A <: Actor with Composable](comb: ArrowOperator[A]): Actor = {
		new CompositionDispatcher(comb)
	}
	
	implicit def lift2ScalaActorRef[A <: Actor with Composable](lift: Lift[A]): ScalaActorRef = {
		lift.actorRef
	}
	
	implicit def lift2ActorRef[A <: Actor with Composable](lift: Lift[A]): ActorRef = {
		lift.actorRef
	}
}
