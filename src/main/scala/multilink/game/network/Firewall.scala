package multilink.game.network

import akka.actor.{Actor, FSM, LoggingFSM}
import akka.util.duration._

import multilink.util.{Composable, ComposableFSM}

object Firewall {
  sealed trait State
	case object Disabled extends State
	case object Bypassed extends State
	case object Active extends State

	sealed trait Messages
	case object DisableFirewall extends Messages 
	case object BypassFirewall extends Messages
	case object EnableFirewall extends Messages
}



class Firewall(x: String) extends Actor with ComposableFSM[Firewall.State, Unit] with LoggingFSM[Firewall.State, Unit] {
  import FSM._
  import Firewall._

  startWith(Active, Unit)

  whenIn(Active) {
    case Event(DisableFirewall,_) =>
      goto(Disabled) 
    case Event(BypassFirewall,_) =>
    	log.info("Bypassing Firewall");
      goto(Bypassed) forMax (2 seconds) 
    case Event(StateTimeout,_) =>
      goto(Disabled) forMax (2 seconds)
  }

  whenIn(Bypassed) {
    case Event(StateTimeout,_) =>
      log.info("Moving to Active")
      goto(Active) forMax (2 seconds)
  }

  whenIn(Disabled) {
    case Event(StateTimeout,_) =>
      log.info("stopping")
      stop
  }

  initialize // this checks validity of the initial state and sets up timeout if needed
}
