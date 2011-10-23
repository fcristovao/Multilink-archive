package multilink.client

import scala.swing._

import akka.actor.Actor._
import akka.actor.Actor
import akka.actor._

import scala.collection.mutable

import multilink.network._
import multilink.software._
import multilink.client._

/*
object Client extends SimpleSwingApplication { 
	def top = new MainFrame {
		title = "First Swing App" 
		contents = new Button {
			text = "Click me"
		}
	}
}
*/

class Client extends Actor {
	val ipdb = actorOf[InternetPointsDatabase].start();
	val dialer = actorOf(new Dialer(ipdb)).start();
	val internic = mutable.Map[Int, ActorRef]()
	
	for(i <- 1 to 10){
		ipdb ! InternetPointsDatabase.Add(i, actorOf(InternetPoint(i)).start())
	}

	val routingList =
		(for(i <- 3 to 7)
			yield i).toList
  	
	def receive() = {
		case _ => dialer ! Dialer.Dial(routingList)
	}
}