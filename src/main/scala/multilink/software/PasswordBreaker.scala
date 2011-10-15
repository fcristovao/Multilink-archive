package multilink.software

import akka.actor.Actor
import akka.actor.ActorRef

import multilink.network.LoginSystem

object PasswordBreaker{
  sealed trait Messages
  
  case class GetPasswordFor(username: LoginSystem.Username, loginSystem: ActorRef) extends Messages
	
}


class PasswordBreaker extends Actor{
	import PasswordBreaker._
	import LoginSystem._
	
	
	def receive = {
		case PasswordBreaker.GetPasswordFor(username, loginSystem) => val tmp = loginSystem !! LoginSystem.GetPasswordFor(username)
			self.reply(tmp.get)
	}

}