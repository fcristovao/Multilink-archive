package multilink.network

import akka.actor.{Actor, FSM, ActorRef}
import akka.actor.Actor._
import akka.routing.Dispatcher


object InternetPoint {
	import multilink.util.Composable._
	
	def apply(ip: Int): Actor = {
		Firewall >>> LoginSystem >>> Gateway
	}
}
