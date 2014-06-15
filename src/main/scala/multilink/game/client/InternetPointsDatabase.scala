package multilink.game.client

import scala.collection.mutable.Map
import akka.actor._
import scaldi.{Injectable, Injector}
import multilink.game.network.internet.InternetPointAddress
import akka.event.LoggingReceive

object InternetPointsDatabase {
	sealed trait Messages
	
	case class Add(id: InternetPointAddress, internetPoint: ActorRef) extends Messages
	case class Get(id: InternetPointAddress) extends Messages
	case class IPFor(id: InternetPointAddress, internetPoint: Option[ActorRef]) extends Messages
}

class InternetPointsDatabaseProducer(implicit injector: Injector) extends IndirectActorProducer with Injectable {
  override def produce() = inject [InternetPointsDatabase]
  override def actorClass = classOf[InternetPointsDatabase]
}

trait InternetPointsDatabase extends Actor

class SimpleInternetPointsDatabase extends InternetPointsDatabase {
	import InternetPointsDatabase._
	
	val database = Map[Int, ActorRef]()
	
	def receive = LoggingReceive {
		case Add(id, internetPoint) => database += id -> internetPoint
		case Get(id) => sender ! IPFor(id, database.get(id))
	}

}