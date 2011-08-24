package multilink.network

import akka.actor.{Actor, FSM}
import akka.event.EventHandler
import akka.util.duration._

sealed trait FirewallState
case object Disabled extends FirewallState
case object Bypassed extends FirewallState
case object Active extends FirewallState


class Firewall extends Actor with FSM[FirewallState, Unit] {
  import FSM._

  startWith(Active, Unit)

  when(Active) {
    case Ev(Disable) =>
      goto(Disabled) 
    case Ev(Bypass) =>
      goto(Bypassed) forMax (2 seconds) 
    case Ev(StateTimeout) =>
      goto(Disabled) forMax (2 seconds)
  }

  when(Bypassed) {
    case Ev(StateTimeout) =>
      EventHandler.info(this, "Moving to Active")
      goto(Active) forMax (2 seconds)
  }

  when(Disabled) {
    case Ev(StateTimeout) =>
      EventHandler.info(this, "stopping")
      stop
  }
  
  

  initialize // this checks validity of the initial state and sets up timeout if needed
}