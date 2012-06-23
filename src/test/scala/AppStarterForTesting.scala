
import akka.actor.Actor._
import akka.actor.Actor
import akka.actor._

import scala.collection._

import multilink.util.composition.Composable._
import multilink.util.composition._
import multilink.util._

object AppStarterForTesting {
  def main(args : Array[String]) : Unit = {
  	
  	ActorSystem("TestSystem").actorOf(Props(new Actor {
			//val ping = Ping(1).lift
			//val pong = Pong(2).lift
	  	//val tmp = ping >>> pong >>> ping >>> (pong &&& ping &&& pong) >>> ping >>> pong >>> ping
	  	
  		val tmp = Ping(1) >>> Pong(1) >>> Ping(2) >>> (Pong(2) &&& Ping(3) &&& Pong(3)) >>> Ping(4) >>> Pong(4) >>> Ping(5)
	  	println(tmp)
	  	
	  	val tmp2 = context.actorOf(Props(tmp))
	  	tmp2 ! "done"
	  	tmp2 ! "reply"
	  	
	  	def receive = {
	  		case msg => println("Got "+msg+"!")
	  	}
  		
  	}))
  }
}