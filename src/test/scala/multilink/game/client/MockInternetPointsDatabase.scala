package multilink.game.client

import akka.actor.ActorRef
import scala.collection.mutable.Map

class MockInternetPointsDatabase(override val database: Map[Int, ActorRef]) extends SimpleInternetPointsDatabase {

}
