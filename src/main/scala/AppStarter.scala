import multilink.network._

import akka.actor.Actor._

object AppStarter {
  def main(args : Array[String]) : Unit = {
    val f = actorOf[Firewall].start()
    
    f ! Bypass
    
    
    
  }
}
