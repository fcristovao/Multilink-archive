package multilink.game.network

import akka.actor.{Actor, FSM, LoggingFSM}
import scala.concurrent.duration._

import multilink.util.composition.{Composable, ComposableFSM}

object Gateway {
  sealed trait State
	case object Disabled extends State
	case object Enabled extends State

	sealed trait Messages
	case class Route(from: Int, through: Int, to: Int) extends Messages
	case class Connect(from: Int, to: Int) extends Messages
	case class Routed(from: Int, through: Int, to: Int) extends Messages
	case class Connected(from: Int, to: Int) extends Messages
	case object DisableGateway extends Messages
	case object EnableGateway extends Messages
}



class Gateway() extends Actor with ComposableFSM[Gateway.State, Unit] with LoggingFSM[Gateway.State, Unit] {
  import FSM._
  import Gateway._

  startWith(Enabled, Unit)

  whenIn(Enabled) {
    case Event(DisableGateway,_) =>
      goto(Disabled) forMax (2 seconds) replying(DisableGateway) replying ("hello")
    case Event(Route(from, through, to),_) =>
    	stay replying Routed(from, through, to)
    case Event(Connect(from, to),_) =>
    	stay replying Connected(from, to)
    case Event(StateTimeout,_) =>
      goto(Disabled) forMax (2 seconds)
  }

  whenIn(Disabled) {
    case Event(DisableGateway,_) =>
      log.info("stopping from disable")
      stop
    case Event(StateTimeout,_) =>
      log.info("stopping")
      stop
  }

  initialize // this checks validity of the initial state and sets up timeout if needed
}