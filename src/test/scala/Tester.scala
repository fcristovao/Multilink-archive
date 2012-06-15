
import akka.actor.Actor._
import akka.actor.Actor
import akka.actor._

import multilink.util._
import multilink.util.Composable._

object Tester {
  def main(args : Array[String]) : Unit = {
  	
  	ActorSystem("TestSystem").actorOf(Props(new Actor {
  		val ping = Lift(Ping(1))
	  	val tmp = ping.onlyOutgoing >>> Pong(2).onlyOutgoing >>> ping
	  	
	  	println(tmp)
	  	
	  	val tmp2 = context.actorOf(Props(tmp))
	  	tmp2 ! "hello"
	  	tmp2 ! "reply"
	  	
	  	
	  	def receive = {
	  		case msg => println("Got "+msg+"!")
	  	}
  		
  	}))
  	
  	
  }
}