package multilink.network

import akka.actor.{Actor, FSM, ActorRef}
import akka.actor.Actor._
import akka.routing.Dispatcher

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
		Lift(LoginSystem())
		val tmp = Lift(new Firewall("test "+ip))
		tmp >>> new Gateway >>> LoginSystem() >>> tmp
	}

}
