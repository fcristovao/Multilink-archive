package multilink.game.client.software

import akka.actor.Actor
import akka.actor.ActorRef

import multilink.game.network.intranet.LoginSystem

object PasswordBreaker{
  sealed trait Messages
  
  case class GetPasswordFor(username: LoginSystem.Username, loginSystem: ActorRef) extends Messages
	
}


class PasswordBreaker extends Actor{
	import PasswordBreaker._
	import LoginSystem._
	
	
	def receive = {
		case x => sender ! x
		/*
		case PasswordBreaker.GetPasswordFor(username, loginSystem) => val tmp = loginSystem ? LoginSystem.GetPasswordFor(username)
			sender ! tmp.get
		*/
	}

}