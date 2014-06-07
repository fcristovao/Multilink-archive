package multilink.util.composition

import akka.actor.{FSM, Actor}
import scala.concurrent.duration._

trait ComposableFSM[S, D] extends FSM[S, D] with Composable {
  this: Actor =>

  import CompositionNetwork._

  protected final val ender: StateFunction = {
    case _ => stay
  }

  def whenIn(stateName: S, stateTimeout: FiniteDuration = null)(stateFunction: StateFunction) = {
    val interceptor: StateFunction = {
      case Event(Process(generation, thisNode, direction, msg), stateData) => {
        val newState = (stateFunction orElse ender)(Event(msg, stateData))
        val doneMsg = List(Done(generation, thisNode, direction, msg))
        newState.replies match {
          case Nil => newState.copy(replies = doneMsg)
          case anythingElse => newState.copy(replies = newState.replies ::: doneMsg)
        }
      }
    }
    super.when(stateName, stateTimeout)(interceptor orElse stateFunction)
  }

}
