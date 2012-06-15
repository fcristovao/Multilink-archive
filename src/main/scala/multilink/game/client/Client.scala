package multilink.game.client

import scala.swing._

import akka.actor.Actor._
import akka.actor.Actor
import akka.actor._

import scala.collection.mutable

import multilink.game.network._
import multilink.game.software._
import multilink.game.client._

import scala.swing._ 
import scala.swing.event._

object Client extends SimpleSwingApplication { 
	val actorSystem = ActorSystem("Multilink")
	val client = actorSystem.actorOf(Props[Client], name = "Client")
	
	val label = new Label {
		text = Dialer.Idle.toString()
	}
	
	def top = new MainFrame {
		title = "First Swing App" 
		val button = new Button {
			text = "Connect" 
		}
		
		contents = new BoxPanel(Orientation.Vertical) {
			contents += button 
			contents += label 
			border = Swing.EmptyBorder(30, 30, 10, 30)
		}
		listenTo(button) 
		reactions += {
			case ButtonClicked(b) =>
				client ! "test"
		}
	}
	
	
}


class Client extends Actor {
	val ipdb = context.actorOf(Props[InternetPointsDatabase],"ipdb")
	val dialer = context.actorOf(Props(new Dialer(this.self, ipdb)),"dialer")
	val internic = mutable.Map[Int, ActorRef]()
	
	for(i <- 1 to 10){
		ipdb ! InternetPointsDatabase.Add(i, context.actorOf(Props(InternetPoint(i)),"InternetPoint(%d)".format(i)))
	}

	val routingList =
		(for(i <- 3 to 7)
			yield i).toList
  	
	def receive() = {
		case x : Dialer.States => Client.label.text = x.toString()
		case _ => dialer ! Dialer.Dial(routingList)
	}
}