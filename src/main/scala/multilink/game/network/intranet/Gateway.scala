package multilink.game.network.intranet

import akka.actor.{Props, Actor, LoggingFSM}
import scala.concurrent.duration._

import multilink.util.composition.ComposableFSM
import multilink.game.network.internet.InternetPointAddress

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
  case object ConnectionRefused extends Messages

  def apply(ip: InternetPointAddress) = {
    Props(classOf[Gateway], ip)
  }
}



class Gateway(ip: InternetPointAddress) extends Actor with ComposableFSM[Gateway.State, Unit] with LoggingFSM[Gateway.State, Unit] {
  import Gateway._

  startWith(Enabled, Unit)

  whenIn(Enabled) {
    case Event(DisableGateway,_) =>
      goto(Disabled) forMax (2 seconds) replying(DisableGateway) replying ("hello")
    case Event(Route(from, through, to),_) if ip == through =>
    	stay replying Routed(from, through, to)
    case Event(Connect(from, to),_) if ip == to =>
    	stay replying Connected(from, to)
    case Event(StateTimeout,_) =>
      goto(Disabled) forMax (2 seconds)
    case _ =>
      stay replying ConnectionRefused
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