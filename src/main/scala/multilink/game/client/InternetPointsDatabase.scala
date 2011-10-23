package multilink.game.client

import scala.collection.mutable.Map
import akka.actor._

object InternetPointsDatabase {
	sealed trait Messages
	
	case class Add(id: Int, internetPoint: ActorRef) extends Messages
	case class Get(id: Int) extends Messages
	case class IPFor(id: Int, internetPoint: Option[ActorRef]) extends Messages
}

class InternetPointsDatabase extends Actor{
	import InternetPointsDatabase._
	
	val database = Map[Int, ActorRef]()
	
	def receive = {
		case Add(id, internetPoint) => database += id -> internetPoint
		case Get(id) => self.reply(IPFor(id, database.get(id)))
	}

}