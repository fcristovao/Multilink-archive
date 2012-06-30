package multilink.util

import akka.actor.Actor
import akka.event.LoggingReceive

trait MultilinkActor extends Actor{
	
	def react : Receive
	
	def receive = LoggingReceive(react)
}

