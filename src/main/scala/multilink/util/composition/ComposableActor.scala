package multilink.util.composition

import akka.actor.Actor
import multilink.util.composition.channels.Channel
import akka.event.LoggingReceive

trait ComposableActor extends Actor with Composable {

  def react: Receive

  override def receive: Receive = LoggingReceive {
    case Channel.Process(generation, thisNode,  msg) => {
      if (react.isDefinedAt(msg)) {
        react(msg)
      }
      sender ! Channel.Done(generation, thisNode, msg)
    }
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