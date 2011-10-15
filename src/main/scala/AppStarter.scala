import multilink.network._
import multilink.software._
import multilink.client._

import akka.actor.Actor._
import akka.actor.Actor
import akka.actor._

import scala.collection._

object AppStarter {
  def main(args : Array[String]) : Unit = {
  	val ipdb = actorOf[InternetPointsDatabase].start();
  	val dialer = actorOf(new Dialer(ipdb)).start();
  	val internic = mutable.Map[Int, ActorRef]()
  	
  	//ipdb.start().start().start()
  	
  	for(i <- 1 to 10){
  		ipdb ! InternetPointsDatabase.Add(i, actorOf(InternetPoint(i)).start())
  	}

  	val routingList =
  		(for(i <- 3 to 7)
  			yield i).toList
  	
  	dialer ! Dialer.Dial(routingList)
  	
  	/*
    val pb = actorOf[PasswordBreaker].start();
    
    val remoteLogin = f !!! InternetPoint.GetLoginSystem
    
    val result = pb !! PasswordBreaker.GetPasswordFor("admin",remoteLogin.get)
    println(result.get)
    */
  }
}


/*

class Test extends Actor {
  val f = actorOf[InternetPoint].start()
  
  def receive = {
    case "Hello" => val tmp= f ! DisableGateway
    	println("here2");
    	//println(tmp.get)
    	//println(tmp.get)
    
    case _ => println("asdasds"); 
  }
  
}

class Test2 extends Actor {

  
  def receive = {
    case "World" => 
    	println("here");
    	self.reply("TEST")
    
    //case DisableGateway => println("Hello"); self.stop
  }
  
}

*/