package multilink.network

import akka.actor.{Actor, FSM, LoggingFSM}
import akka.event.EventHandler
import akka.util.duration._
import multilink.util.{Composable, ComposableFSM}



object Gateway {
  sealed trait State
	case object Disabled extends State
	case object Enabled extends State

	sealed trait Messages
	case class Route(from: Int, through: Int, to: Int) extends Messages
	case class Connect(from: Int, to: Int) extends Messages
	case class Routed(from: Int, through: Int, to: Int) extends Messages
	case class Connected(from: Int, to: Int) extends Messages
}



class Gateway() extends Actor with ComposableFSM[Gateway.State, Unit] with LoggingFSM[Gateway.State, Unit] {
  import FSM._
  import Gateway._

  startWith(Enabled, Unit)

  whenIn(Enabled) {
    case Ev(DisableGateway) =>
      goto(Disabled) forMax (2 seconds) replying(DisableGateway) replying ("hello")
    case Ev(Route(from, through, to)) =>
    	stay replying Routed(from, through, to)
    case Ev(Connect(from, to)) =>
    	stay replying Connected(from, to)
    case Ev(StateTimeout) =>
      goto(Disabled) forMax (2 seconds)
  }

  whenIn(Disabled) {
    case Ev(DisableGateway) =>
      EventHandler.info(this, "stopping from disable")
      stop
    case Ev(StateTimeout) =>
      EventHandler.info(this, "stopping")
      stop
  }

  initialize // this checks validity of the initial state and sets up timeout if needed
}