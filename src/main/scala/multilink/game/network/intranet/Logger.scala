package multilink.game.network.intranet

import multilink.util.composition.ComposableFSM

object Logger {
  sealed trait State
  case object Active extends State

  sealed trait Messages
  case object GetLogs extends Messages
  case class Logs(logs: Data) extends Messages

  type Data = List[Any]
}

class Logger extends ComposableFSM[Logger.State, Logger.Data] {
  import Logger._

  startWith(Active, List())

  whenIn(Active) {
    case Event(GetLogs, currentLogs) =>
      stay() replying Logs(currentLogs)
    case Event(anyMsg, currentLog) =>
      stay() using (currentLog :+ anyMsg)
  }

  initialize()
}

