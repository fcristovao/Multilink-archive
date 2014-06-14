package multilink.game.network.intranet

import multilink.util.composition.ComposableFSM
import multilink.game.network.intranet.AccessControl.{Credentials, Config}
import scala.collection.immutable.Map
import akka.actor.{ActorRef, Props}

object AccessControl {
  import scala.concurrent.duration._

  type Username = String
  type Password = String

  type Credentials = Map[Username, Password]

  sealed trait Messages
  case class AddUsernameAndPassword(username: Username, password: Password) extends Messages
  case class Login(username: Username, password: Password) extends Messages
  case class AccessGranted(username: Username) extends Messages
  case object AccessDenied extends Messages
  case class SessionTimeout(actor: ActorRef) extends Messages

  sealed trait State
  case object Active extends State

  case class Data(credentials: Credentials, sessions: Set[ActorRef])

  def apply(credentials: Credentials)(implicit config: Config = Config()) = {
    Props(classOf[AccessControl], credentials, config)
  }

  case class Config(sessionTimeout: FiniteDuration = 60 seconds)

}

class AccessControl(initialCredentials: Credentials, config: Config) extends ComposableFSM[AccessControl.State, AccessControl.Data] {
  import AccessControl._

  startWith(Active, Data(initialCredentials, Set()))

  whenIn(Active) {
    case Event(Login(username, tryPassword), data@Data(credentials, sessions)) =>
      credentials.get(username) match {
        case None => // Unknown user
          sender() ! AccessDenied
          stay()
        case Some(password) if password != tryPassword => // wrong password
          sender() ! AccessDenied
          stay()
        case Some(password) if password == tryPassword => // correct password
          setTimer(sender().path.toString, SessionTimeout(sender()), config.sessionTimeout)
          sender() ! AccessGranted(username)
          stay() using data.copy(sessions = sessions + sender())

      }
    case Event(AddUsernameAndPassword(username, password), data@Data(credentials, _)) => {
      stay() using data.copy(credentials = credentials + (username -> password))
    }
    case Event(SessionTimeout(timedOutSession), data@Data(_, sessions)) => {
      stay() using data.copy(sessions = sessions - timedOutSession)
    }
    case Event(msg, Data(_, sessions)) => {
      if (sessions.contains(sender())) {
        //renew the session timer:
        setTimer(sender().path.toString, SessionTimeout(sender()), config.sessionTimeout)
        stay()
      } else {
        stay() replying AccessDenied
      }
    }
  }

  override def toString = "AccessControl"
}

