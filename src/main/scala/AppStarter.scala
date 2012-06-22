import multilink.game.client._

import akka.actor.Actor._
import akka.actor.Actor
import akka.actor._

import scala.collection._

object AppStarter {
  def main(args : Array[String]) : Unit = {
  	
  	//val client = actorOf[Client].start();
  	
  	//client ! "something"
  	
  	Client.main(args)
  }
}