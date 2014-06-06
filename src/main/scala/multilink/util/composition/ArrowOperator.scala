package multilink.util.composition

import akka.actor.{Actor, ActorRef, Props}

sealed trait ArrowOperator[-A <: Actor with Composable]{
	def lift: ArrowOperator[A]
	def >>>[B <: A](other: ArrowOperator[B]): ArrowOperator[B]
	def &&&[B <: A](other: ArrowOperator[B]): ArrowOperator[B] 
	
	def incoming: Boolean
	def outgoing: Boolean
	
	def alsoOutgoing: ArrowOperator[A] 
	def onlyOutgoing: ArrowOperator[A] 
}

object Lift{
	import scala.concurrent.Await
	import akka.pattern.ask
	import akka.actor.ActorRefFactory
	import akka.actor.ActorSystem
	import akka.actor.ActorContext
  import scala.concurrent.duration._
	
	private case class LifterActor() extends Actor {
		// It just provides an unique number for each pair (ActorRefFactory, String)
		import scala.collection.mutable.Map
		
		val mapOfTuples = Map[(ActorRefFactory, String), Int]()
		
		def receive = {
			case (actorRefFactory: ActorRefFactory, name: String) => {
				val tuple = (actorRefFactory,name)
				val newID = mapOfTuples.getOrElse(tuple,-1) + 1
				sender ! newID
				mapOfTuples(tuple) = newID
			}
		}
	} 
	
	var lifterActor: Option[ActorRef] = None 
	
	def actorNameFromManifest[A <: Actor with Composable](manifest: Manifest[A]) = manifest.toString().split('.').last
	
	def apply[A <: Actor with Composable](actorFactory: () => A, actorRefFactory: ActorRefFactory, manifest: Manifest[A]) = {
		val lifter = lifterActor match {
			case None => {
				val actorSystem = actorRefFactory match {
					case actorSystem : ActorSystem => actorSystem
					case actorContext : ActorContext => actorContext.system
				}
				lifterActor = Some(actorSystem.actorOf(Props(new LifterActor),"lifter"))
				lifterActor.get
			}
			case Some(lifterActor) => lifterActor
		}
		
		val actorName = actorNameFromManifest(manifest)
		val id : Int = Await.result(ask(lifter,(actorRefFactory, actorName))(5 seconds).mapTo[Int], Duration.Inf)
		val finalActorName = actorName + (if(id==0) "" else id.toString)
		
		val actorRef = actorRefFactory.actorOf(Props(actorFactory), finalActorName)
		
		new Lift[A](actorRef,actorName)
	}
}

case class Lift[A <: Actor with Composable] (val actorRef: ActorRef, val actorName: String, override val incoming: Boolean = true, override val outgoing: Boolean = false) extends ArrowOperator[A] {
	def lift = this
	
	def >>>[B <: A](other: ArrowOperator[B]): ArrowOperator[B] = {
		other match {
			case Lift(_,_,_,_) | Splitter(_) => Composition[B](List(this, other))
			case Composition(composables) => Composition(this :: composables)
		}
	}
	
	def &&&[B <: A](other: ArrowOperator[B]): ArrowOperator[B] = {
		Splitter(List(this, other))
	}
	
	def alsoOutgoing: ArrowOperator[A] = copy(incoming = true, outgoing = true)
	def onlyOutgoing: ArrowOperator[A] = copy(incoming = false, outgoing = true)

	override val toString: String = {
		"⇑("+actorName+")"
	}
}

case class Composition[A <: Actor with Composable](composables: List[ArrowOperator[A]]) extends ArrowOperator[A] {
	def lift = this
	
	def >>>[B <: A](other: ArrowOperator[B]): ArrowOperator[B] = {
		other match {
			case Lift(_,_,_,_) | Splitter(_) => Composition(composables :+ other)
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
	
	override val toString: String = {
		"("+(composables mkString " >>> ")+")"
	}
}

case class Splitter[A <: Actor with Composable](splittedInto: List[ArrowOperator[A]]) extends ArrowOperator[A] {
	def lift = this
	
	def >>>[B <:A](other: ArrowOperator[B]): ArrowOperator[B] = {
		other match {
			case Lift(_,_,_,_) | Splitter(_) => Composition(List(this, other))
			case Composition(composables) => Composition(this :: composables)
		}
	}
	def &&&[B <: A](other: ArrowOperator[B]): ArrowOperator[B] = {
		other match {
			case Lift(_,_,_,_) | Composition(_) => Splitter(splittedInto :+ other)
			case Splitter(otherSplittedInto) => Splitter(splittedInto ::: otherSplittedInto)
		}
	}
	
	lazy val incoming: Boolean = splittedInto.exists(_.incoming) 
	lazy val outgoing: Boolean = splittedInto.exists(_.outgoing)
	
	def alsoOutgoing: ArrowOperator[A] = copy(splittedInto = splittedInto.map(_.alsoOutgoing))
	def onlyOutgoing: ArrowOperator[A] = copy(splittedInto = splittedInto.map(_.onlyOutgoing))
	
	override val toString: String = {
		"("+(splittedInto mkString " &&& ")+")"
	}
}
