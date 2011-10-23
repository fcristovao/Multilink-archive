package multilink.game.network

import akka.actor.Actor

import scala.collection.mutable.Map

import multilink.util.{Composable, ComposableActor, LoggableComposableActor}

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

case class LoginSystem() extends ComposableActor with LoggableComposableActor{
	import LoginSystem._
  
  val database = Map[Username, Password]()
  
  def process = {
		case GetPasswordFor(username) => reply(
    		database.get(username) match {
    			case None => UsernameDoesntExist(username)
    			case Some(password) => PasswordForUsername(username, password)
    		}
    )
    case AddUsernameAndPassword(username, password) => {
    	database += username -> password
    	done
    }
	}

}