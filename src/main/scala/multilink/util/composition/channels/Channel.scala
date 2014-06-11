package multilink.util.composition.channels

import akka.actor._
import multilink.util.composition.channels.NetworkGraph.NetworkNode


object Channel {

  sealed abstract class Messages(generation: Int)
  case class Process(generation: Int, node: NetworkNode, msg: Any) extends Messages(generation)
  case class Done(generation: Int, node: NetworkNode, msg: Any) extends Messages(generation)

  sealed trait State
  case object Idle extends State
  case object WaitingForNodeDone extends State

  sealed trait Data
  case class Processing(replyTo: ActorRef, node: ActorRef, hadAnswers: Boolean) extends Data
  case object NullData extends Data
  case object HadAnswers extends Data
  case object NoAnswers extends Data
}

class Channel(id: Int, networkSource: NetworkNode) extends Actor with
                                                           MyUnboundedStash with
                                                           FSM[Channel.State, Channel.Data] with
                                                           LoggingFSM[Channel.State, Channel.Data] {
  import Channel._

  private val generation = Iterator from 0

  startWith(Idle, NullData)

  when(Idle) {
    case Event(msg, _) => {
      val nextGen = generation.next()
      networkSource.actor ! Process(nextGen, networkSource, msg)
      goto(WaitingForNodeDone) using Processing(sender(), networkSource.actor, hadAnswers = false)
    }
  }

  when(WaitingForNodeDone) {
    case Event(Done(currentGen, node, msg), data@Processing(_, _, false)) => {
      node.next match {
        case Nil =>
          goto(Idle)
        case List(nextNode) =>
          nextNode.actor ! Process(currentGen, nextNode, msg)
          stay() using data.copy(node = nextNode.actor)
      }
    }
    case Event(Done(_, _, _), Processing(answerTo, currentProcessingActor, true)) => {
      goto(Idle) using NullData
    }
    case Event(msg, data@Processing(answerTo, currentProcessingActor, _)) if sender() == currentProcessingActor => {
      answerTo ! msg
      stay() using data.copy(hadAnswers = true)
    }
    case Event(msg, data@Processing(answerTo, currentProcessingActor, _)) if sender() != currentProcessingActor => {
      stash()
      stay()
    }
  }

  onTransition {
    case _ -> Idle => unstash()
  }

  initialize()

  override def toString = "Channel(" + id + ")"
}
