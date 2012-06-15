package multilink.util


case class Ping(val id: Int) extends ComposableActor {
	
	val iter = Iterator from 1
	
	def process = {
		case msg @ "hello" => println("Ping("+id+") "+iter.next()+"th msg received: "+msg); done
		case msg @ "reply" => println("Ping("+id+") "+iter.next()+"th msg received: "+msg); reply("hello")
	}

}

case class Pong(val id: Int) extends ComposableActor {
	
	val iter = Iterator from 1
	
	def process = {
		case msg @ "hello" => println("Pong("+id+") "+iter.next()+"th msg received: "+msg); reply("world")
		case msg @ "reply" => println("Pong("+id+") "+iter.next()+"th msg received: "+msg); reply("hello")
	}

}