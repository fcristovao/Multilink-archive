import multilink.network._
import multilink.software._
import multilink.client._

import akka.actor.Actor._
import akka.actor.Actor
import akka.actor._

import scala.collection._

import multilink.util.Composable._

object AppStarter {
  def main(args : Array[String]) : Unit = {
  	
  	val client = actorOf[Client].start();
  	
  	client ! "something"
  }
}