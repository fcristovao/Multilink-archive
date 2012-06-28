package multilink.util

import akka.actor.Actor

trait MultilinkActor extends Actor {
	
	def react : Receive
	
	def receive = react
}

