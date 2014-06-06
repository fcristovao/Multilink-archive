package multilink.game.network

import akka.actor.{Actor, FSM, ActorRef}
import akka.actor.Actor._
import akka.actor.ActorRefFactory
import multilink.util.replication.Replicator
import akka.actor.Props


object InternetPoint{ //} extends Actor{
	import multilink.util.composition.Composable._
	
	/* What we want to be able to do: 
	 * (If one could also take the () after each class, it would be nice)
	 */
	/*
	val logger = Lift(Logger());
	val allSystem = logger.onlyOutBound >>>
		Gateway() >>>
		Firewall() >>>
		LoginSystem() >>>
		(FileServer() &&& logger &&& Console() )
	*/
	
}


class InternetPoint(ip: Int) extends Replicator[InternetPoint]{
	
	val systems = {
		val tmp = context
		val logger = new Logger().lift
		val allSystem = 
			logger.onlyOutgoing >>>
			new Gateway() >>>
			new Firewall("test") >>>
			new LoginSystem ///>>>
			//(new FileServer() &&& logger &&& Console())
			
		context.actorOf(Props(allSystem),"systems")
	}
	
	def react() = {
		case msg => systems.forward(msg)
	}
}