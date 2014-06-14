package multilink.util.composition

import akka.actor.{LoggingFSM, FSM, Actor}
import scala.concurrent.duration._
import multilink.util.composition.channels.Channel.{Done, Process}

trait ComposableFSM[S, D] extends FSM[S, D] with LoggingFSM[S, D] with Composable {
  this: Actor =>

  protected val ender: StateFunction = {
    case _ => stay()
  }

  def whenIn(stateName: S, stateTimeout: FiniteDuration = null)(stateFunction: StateFunction) = {
    val interceptor: StateFunction = {
      case Event(Process(generation, thisNode, msg), stateData) => {
        val doneMsg = Done(generation, thisNode, msg)
        (stateFunction orElse ender)(Event(msg, stateData)) replying doneMsg
      }
    }
    super.when(stateName, stateTimeout)(interceptor orElse stateFunction)
  }

}
