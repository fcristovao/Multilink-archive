package multilink.util

import composition._
import akka.actor.Actor
import multilink.util.replication.ReplicatableActor

case class Ping(val id: Int) extends MultilinkActor with ComposableActor{
	
	val iter = Iterator from 1
	
	def getState = 0
	def setState(x: Int) : Unit = 0
	
	def react = {
		case msg @ "done" => println("Ping("+id+") "+iter.next()+"th msg received: "+msg); 
		case msg @ "reply" => println("Ping("+id+") "+iter.next()+"th msg received: "+msg); 
	} 

}

case class Pong(val id: Int) extends MultilinkActor with ComposableActor{
	
	val iter = Iterator from 1
	
	def react = {
		case msg @ "done" => println("Pong("+id+") "+iter.next()+"th msg received: "+msg); 
		case msg @ "reply" => println("Pong("+id+") "+iter.next()+"th msg received: "+msg); sender ! ("ReplyFrom"+this.toString)
	}

}

