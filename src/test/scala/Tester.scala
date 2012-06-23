
import akka.actor.Actor._
import akka.actor.Actor
import akka.actor._

import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfterAll

import multilink.util.composition.Composable._
import multilink.util.composition._
import multilink.util._

class TestingSuite extends FunSuite with BeforeAndAfterAll{

	val actorSystem = ActorSystem("TestSystem")
	
	override def beforeAll {
	}
	
	test("Just testing!"){
		
		actorSystem.actorOf(Props(new Actor {
			val ping = Ping(1).lift
	  	val tmp = ping.onlyOutgoing >>> Pong(2).onlyOutgoing >>> ping
	  	
	  	println(tmp)
	  	
	  	/*
	  	val tmp2 = context.actorOf(Props(tmp))
	  	tmp2 ! "hello"
	  	tmp2 ! "reply"
	  	*/
	  	
	  	def receive = {
	  		case msg => println("Got "+msg+"!")
	  	}
  		
  	}))
	}
}