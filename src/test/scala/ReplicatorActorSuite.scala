
import akka.actor.Actor._
import akka.actor.{Actor, ActorLogging}
import akka.actor._
import multilink.util.replication._
import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfterAll
import multilink.util.MultilinkActor
import multilink.game.network.InternetPoint
import akka.testkit.TestKit
import akka.testkit.ImplicitSender

object ReplicatorActorSuite {
	class ReplicatorActorTest extends MultilinkActor with Replicator[ReplicatorActorTest]{
		def react = {
			case x => 
		}
	}
}


class ReplicatorActorSuite extends TestKit(ActorSystem("TestSystem")) with ImplicitSender with FunSuite with BeforeAndAfterAll{
	
	override def afterAll{
		system.shutdown()
	}
	
	ignore("Test the replication of messages"){
		val testActor = system.actorOf(Props(new InternetPoint(1)))
		
		testActor ! "qazwsx"
	}
}

