package multilink.util

import composition._

case class Ping(val id: Int) extends ComposableActor {
	
	val iter = Iterator from 1
	
	def process = {
		case msg @ "done" => println("Ping("+id+") "+iter.next()+"th msg received: "+msg); done
		case msg @ "reply" => println("Ping("+id+") "+iter.next()+"th msg received: "+msg); reply("ReplyFrom"+this.toString)
	} 

}

case class Pong(val id: Int) extends ComposableActor {
	
	val iter = Iterator from 1
	
	def process = {
		case msg @ "done" => println("Pong("+id+") "+iter.next()+"th msg received: "+msg); done
		case msg @ "reply" => println("Pong("+id+") "+iter.next()+"th msg received: "+msg); reply("ReplyFrom"+this.toString)
	}

}