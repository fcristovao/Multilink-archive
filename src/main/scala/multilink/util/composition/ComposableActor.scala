package multilink.util.composition

import akka.actor.Actor
import CompositionNetwork._
import akka.event.LoggingReceive

trait ComposableActor extends Actor with Composable {

  abstract override def receive: Receive = LoggingReceive {
    case Process(generation, thisNode, direction, msg) => {
      if (super.receive.isDefinedAt(msg)) {
        super.receive(msg)
      }
      sender ! Done(generation, thisNode, direction, msg)
    }
    case msg if super.receive.isDefinedAt(msg) => super.receive(msg)
  }

}

/*
trait LoggableComposableActor extends ComposableActor with ActorLogging{
	override abstract def receive : Receive = {
		case msg @ Process(_, _, direction, realMsg) => {
			val isHandled = process.isDefinedAt(realMsg) 
			log.debug("Received "+ (if(isHandled) "" else "un") +"handled "+direction+" message "+ realMsg)
			super.receive(msg)
		}
	}
		
}
*/