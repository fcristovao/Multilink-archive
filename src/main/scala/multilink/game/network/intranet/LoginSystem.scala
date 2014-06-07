package multilink.game.network.intranet

import scala.collection.mutable.Map
import multilink.util.composition.{Composable, ComposableActor}

object LoginSystem{
  type Username = String
  type Password = String
  
  //val Username.ADMIN = "admin"
  
  sealed trait Messages
  
  case class GetPasswordFor(username: Username) extends Messages // to be used by the passwordBreakers
  case class UsernameDoesntExist(username: Username) extends Messages
  case class PasswordForUsername(username: Username, password: Password) extends Messages
  case class AddUsernameAndPassword(username: Username, password: Password) extends Messages
}

/*
class LoginSystem extends Actor{
	import LoginSystem._
  
  val database = Map[Username, Password]()
  
  def receive = {
    case GetPasswordFor(username) => self.reply(
    		database.get(username) match {
    			case None => UsernameDoesntExist(username)
    			case Some(password) => PasswordForUsername(username, password)
    		}
    )
    case AddUsernameAndPassword(username, password) => {
    	database += username -> password
    }
    
  }

}
*/

trait LoginSystem extends ComposableActor {
	import LoginSystem._
  
	def getState = ()
	def setState(x: Unit) : Unit = ()
	
  val database = Map[Username, Password]()
  
  abstract override def receive = {
		case GetPasswordFor(username) => {
    	val answer = 
    		database.get(username) match {
    			case None => UsernameDoesntExist(username)
    			case Some(password) => PasswordForUsername(username, password)
    		}
    	sender ! answer
		}
    case AddUsernameAndPassword(username, password) => {
    	database += username -> password
    }
	}

}

