
import akka.actor.Actor._
import akka.actor.{Actor, ActorLogging}
import akka.actor._

import multilink.util.replication._

import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfterAll

class ReplicatorActorSuite extends FunSuite with BeforeAndAfterAll{
	val actorSystem = ActorSystem("TestSystem")
	
	
	class TestActor extends Actor {
		val nullPF = new PartialFunction[Any, Unit] { 
			def isDefinedAt(v: Any) = false; 
			def apply(v: Any) = throw new MatchError 
		} 
		
		def receive: Receive = nullPF
	}
	
	class ReplicatorActorTest extends TestActor with Replicator{
		//override def receive = {
		//	case x => 
		//}
	}
	
	override def beforeAll {
		
	}
	
	
	test("Test the replication of messages"){
		val testActor = actorSystem.actorOf(Props(new ReplicatorActorTest))
		
		testActor ! "qazwsx"
	}
}