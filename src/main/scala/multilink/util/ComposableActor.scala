package multilink.util

import akka.event.EventHandler
import akka.actor.Actor
import akka.actor.Actor._

import akka.config.Config.config

import Composable._

trait ComposableActor extends Actor with Combinable {
	this: Actor =>
	
	
	private val defaultFunction: PartialFunction[Any,ComposableMessages] = {
		case _ => done
	}	
	
	def reply(x: Any) = Reply(x)
	def done() = Done
	
	def process: PartialFunction[Any, ComposableMessages] 
	
	protected override def receive: Receive = {
		case msg => 
			self.reply((process orElse defaultFunction)(msg))
	}
}

trait LoggableComposableActor extends ComposableActor {
	
	val debugMsg = config.getBool("akka.actor.debug.receive", false)
	
	override abstract def receive : Receive = {
		case msg => {
			if(debugMsg){
				val isHandled = process.isDefinedAt(msg) 
				EventHandler.debug(this, "received "+ (if(isHandled) "" else "un") +"handled message "+msg)
			}
			super.receive(msg)
		}
	}
		
}