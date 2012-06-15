package multilink.game.network

import akka.actor.{Actor, FSM, ActorRef}
import akka.actor.Actor._

object InternetPoint {
	import multilink.util.Composable._
	
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
	
	def apply(ip: Int): Actor = {
		new Logger().onlyOutgoing >>> new Gateway() >>> new Firewall("test "+ip) >>> LoginSystem() 
	}

}
